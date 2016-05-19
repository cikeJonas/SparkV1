package org.apache.spark.v1

import java.util.Random

import org.apache.spark.{HashPartitioner, _}
import org.apache.spark.annotation.{DeveloperApi, Since}
import org.apache.spark.rdd.{CoalescedRDD, PartitionwiseSampledRDD, ShuffledRDD, _}
import org.apache.spark.storage.{RDDBlockId, StorageLevel}
import org.apache.spark.util.Utils
import org.apache.spark.util.random.{BernoulliCellSampler, BernoulliSampler, PoissonSampler}

import scala.reflect.ClassTag
import scala.util.Random

abstract class RDD[T: ClassTag](
                                 @transient private var _sc: SparkContext,
                                 @transient private var deps: Seq[Dependency[_]]

                               ) extends Serializable with Logging {
  if (classOf[RDD[_]].isAssignableFrom(elementClassTag.runtimeClass)) {
    // This is a warning instead of an exception in order to avoid breaking user programs that
    // might have defined nested RDDs without running jobs with them
    logWarning("Spark does not support nested RDDs (see SPARK-5063")
  }

  private def sc: SparkContext = {
    if (_sc == null) {
      throw new SparkException(
        "This RDD lacks a SparkContext. It could happen in the following cases: \n (1) RDD" +
          "transformations and actions are NOT invoked by the driver, but inside of other " +
          "transformations; for example, rdd1.map(x => rdd2.values.count() * x) is invalid " +
          "because the values transformation and count action cannot be performed inside of the " +
          "rdd1.map transformation. For more information, see SPARK-5063.\n (2) When a Spark" +
          "Streaming job recovers from checkpoint, this exception will be hit if a reference to " +
          "an RDD not defined by the streaming job is used in DStream operations. For more " +
          "information, See SPARK-13758"
      )
    }
    _sc
  }

  /**
    * Construct an RDD with just a one-to-one dependency on one parent
    */
  def this(@transient oneParent: RDD[_]) =
    this(oneParent.context, List(new OneToOneDependency(oneParent)))

  private[rdd] def conf = sc.conf

  // =================================================================

  // Methods that should be implemented by subclasses of RDD

  // =================================================================

  /**
    * :: DeveloperApi ::
    * Implemented by subclasses to compute a given partition.
    */
  @DeveloperApi
  def compute(split: Partition, context: TaskContext): Iterator[T]

  /**
    * Implemented by subclasses to renturn the set of partitions in this RDD. This method will only
    * be called once,so it is sage to implement a time-consuming computation in it.
    *
    * The partition in this array must satisfy the following property:
    *
    * 'rdd.partitions.zipWithIndex.forall { case (partition, index) => partition.index == index }'
    *
    */

  protected def getPartitions: Array[Partition]

  /**
    * Implemented by subclassed to return how this RDD depends on parent RDDs. This method will only
    * be called once, so it is safe to implement a time-consuming computation in it.
    *
    */
  protected def getDependencies: Array[Partition]

  /**
    * Optionally overridden by subclasses to specify placement preferences.
    */
  protected def getPreferredLocations(split: Partition): Seq[String] = Nil

  /** Optionally overridden by subclasses to specify how they are partitioned. */
  @transient val partition: Option[Partitioner] = None


  // =======================================================================
  // Methods and fields available on all RDDs
  // =======================================================================

  /** The SparkContext that created this RDD. */
  def sparkContext: SparkContext = sc

  /** A unique ID for this RDD (within its SparkContext). */
  val id: Int = sc.newRddId()

  /** A friendly name for this RDD */
  @transient var name: String = null

  /** Assign a name to this RDD */
  def setName(_name: String): this.type = {
    name = _name
    this
  }

  /**
    * Mark this RDD for persisting using the specified level.
    *
    * @param newLevel the target storage level
    * @param allowOverride whether to override any existing level with the new one
    */
  private def persist(newLevel: StorageLevel, allowOverride: Boolean): this.type = {
    // TODO: Handle changes of StorageLevel
    if (storageLevel != StorageLevel.NONE && newLevel != storageLevel && !allowOverride) {
      throw new UnsupportedOperationException(
        "Cannot change storage level of an RDD after it was already assigned a level")
    }
    // If this is the first time this RDD is marked for persisting, register it
    // with the SparkContext for cleanups and accounting. Do this only once.
    if (storageLevel == StorageLevel.NONE) {
      sc.cleaner.foreach(_.registerRDDForCleanup(this))
      sc.persistRDD(this)
    }
    storageLevel = newLevel
    this
  }

  /**
    * Set this RDD's storage level to persist its values across operations after the first time
    * it is computed. This can only be used to assign a new storage level if the RDD does not
    * have a storage level set yet. Local checkpointing is an exception.
    */
  def persist(newLevel: StorageLevel): this.type = {
    if (isLocallyCheckpointed) {
      // This means the user previously called localCheckpoint(), which should have already
      // marked this RDD for persisting. Here we should override the old storage level with
      // one that is explicitly requested by the user (after adapting it to use disk).
      persist(LocalRDDCheckpointData.transformStorageLevel(newLevel), allowOverride = true)
    } else {
      persist(newLevel, allowOverride = false)
    }
  }

  /** Persist this RDD with the default storage level (`MEMORY_ONLY`). */
  def persist(): this.type = persist(StorageLevel.MEMORY_ONLY)

  /** Persist this RDD with the default storage level (`MEMORY_ONLY`). */
  def cache(): this.type = persist()

  /**
    * Mark the RDD as non-persistent, and remove all blocks for it from memory and disk.
    *
    * @param blocking Whether to block until all blocks are deleted.
    * @return This RDD.
    */
  def unpersist(blocking: Boolean = true): this.type = {
    logInfo("Removing RDD " + id + " from persistence list")
    sc.unpersistRDD(id, blocking)
    storageLevel = StorageLevel.NONE
    this
  }

  /** Get the RDD's current storage level, or StorageLevel.NONE if none is set. */
  def getStorageLevel: StorageLevel = storageLevel

  // Our dependencies and partitions will be gotten by calling subclass's methods below, and will
  // be overwritten when we're checkpointed
  private var dependencies_ : Seq[Dependency[_]] = null
  @transient private var partitions_ : Array[Partition] = null

  /** An Option holding our checkpoint RDD, if we are checkpointed */
  private def checkpointRDD: Option[CheckpointRDD[T]] = checkpointData.flatMap(_.checkpointRDD)

  /**
    * Get the list of dependencies of this RDD, taking into account whether the
    * RDD is checkpointed or not.
    */
  final def dependencies: Seq[Dependency[_]] = {
    checkpointRDD.map(r => List(new OneToOneDependency(r))).getOrElse {
      if (dependencies_ == null) {
        dependencies_ = getDependencies
      }
      dependencies_
    }
  }

  /**
    * Get the array of partitions of this RDD, taking into account whether the
    * RDD is checkpointed or not.
    */
  final def partitions: Array[Partition] = {
    checkpointRDD.map(_.partitions).getOrElse {
      if (partitions_ == null) {
        partitions_ = getPartitions
        partitions_.zipWithIndex.foreach { case (partition, index) =>
          require(partition.index == index,
            s"partitions($index).partition == ${partition.index}, but it should equal $index")
        }
      }
      partitions_
    }
  }

  /**
    * Returns the number of partitions of this RDD.
    */
  @Since("1.6.0")
  final def getNumPartitions: Int = partitions.length

  /**
    * Get the preferred locations of a partition, taking into account whether the
    * RDD is checkpointed.
    */
  final def preferredLocations(split: Partition): Seq[String] = {
    checkpointRDD.map(_.getPreferredLocations(split)).getOrElse {
      getPreferredLocations(split)
    }
  }

  /**
    * Internal method to this RDD; will read from cache if applicable, or otherwise compute it.
    * This should ''not'' be called by users directly, but is available for implementors of custom
    * subclasses of RDD.
    */
  final def iterator(split: Partition, context: TaskContext): Iterator[T] = {
    if (storageLevel != StorageLevel.NONE) {
      getOrCompute(split, context)
    } else {
      computeOrReadCheckpoint(split, context)
    }
  }

  /**
    * Return the ancestors of the given RDD that are related to it only through a sequence of
    * narrow dependencies. This traverses the given RDD's dependency tree using DFS, but maintains
    * no ordering on the RDDs returned.
    */
  private[spark] def getNarrowAncestors: Seq[RDD[_]] = {
    val ancestors = new mutable.HashSet[RDD[_]]

    def visit(rdd: RDD[_]) {
      val narrowDependencies = rdd.dependencies.filter(_.isInstanceOf[NarrowDependency[_]])
      val narrowParents = narrowDependencies.map(_.rdd)
      val narrowParentsNotVisited = narrowParents.filterNot(ancestors.contains)
      narrowParentsNotVisited.foreach { parent =>
        ancestors.add(parent)
        visit(parent)
      }
    }

    visit(this)

    // In case there is a cycle, do not include the root itself
    ancestors.filterNot(_ == this).toSeq
  }

  /**
    * Compute an RDD partition or read it from a checkpoint if the RDD is checkpointing.
    */
  private[spark] def computeOrReadCheckpoint(split: Partition, context: TaskContext): Iterator[T] =
  {
    if (isCheckpointedAndMaterialized) {
      firstParent[T].iterator(split, context)
    } else {
      compute(split, context)
    }
  }

  /**
    * Set this RDD's storage level to persist its values across operations after the first time
    * it is computed.This can only be used to assign a new Storage level if the RDD does not have
    * a storage level set yet.Local checkpointing is an exception.
    */

  def persist(newLevel: StorageLevel): this.type = {
    if (isLocallyCheckpointed) {
      // This means the user previously called localCheckpoint(), which should have already
      // marked this RDD for persisting. Here we should override the old storage level whit
      // ont thate is explicitly requested by the user (after adapting it to use disk).
      persist(LocalRddCheckpointData.transformStorageLevel(newLevel), allowOverride = true)
    } else {
      persist(newLevel, allowOverride = false)
    }

  }

  /**
    * Persist this RDD with the default storage level ('MEMORY_ONLY').
    */

  def persist(): this.type = persist(StorageLevel.MEMORY_ONLY)

  // Persist this RDD with the default storage level ('MEMORY_ONLY').
  def cache(): this.type = persist()

  /**
    * Mark the RDD as non-persistent, and remove all blocks for it from memory and disk
    *
    *
    */
  def unpersist(blocking: Boolean = true): this.type = {
    logInfo("Removing RDD " + id + " From persistence list")
    sc.unpersistRDD(id, blocking)
    storageLevel = StorageLevel.NONE
    this
  }

  /** Get the RDD's current storage level, or StorageLevel.NONE if none is set. */
  def getStorageLevel: StorageLevel = storageLevel

  // Our dependencies and partitions will be gotten by calling subclass's methods below, and will
  // be overwritten when we're checkpointed
  private var dependencies_ : Seq[Dependency[_]] = null
  @transient private var partitions_ : Array[Partition] = null

  /** An Option holding our checkpoint RDD, if we are checkpointed */
  private def checkpointRDD: Option[CheckpointRDD[T]] = checkpointRDD.flatMap(_.checkpointRDD)

  /**
    * Get the list of dependencies of this RDD, taking into account whether the
    * RDD if checkpoined or not.
    */
  final def dependencies: Seq[Dependency[_]] = {
    checkpointRDD.map(r => List(new OneToOneDependency(r))).getOrElse {
      if (dependencies_ == null) {
        dependencies_ = getDependencies
      }
      dependencies_
    }
  }


  /**
    * Get the array of partitions of this RDD, taking into account whether the
    * RDD is checkpointed or not.
    */
  final def partitions: Array[Partition] = {
    checkpointRDD.map(_.partitions).getOrElse {
      if (partitions_ == null) {
        partitions_ = getPartitions
        partitions_.zipWithIndex.foreach { case (partition, index) =>
          require(partition.index == index,
            s"partitions($index).partition == ${partition.index}, but it should equal $index")
        }
      }
      partitions_
    }
  }

  /**
    * Returns the number of partitions of this RDD.
    */
  @Since("1.6.0")
  final def getNumPartitions: Int = partitions.length

  /**
    * Get the preferred locations of a partition, taking into account whether the
    * RDD is checkpointed.
    */
  final def preferredLocations(split: Partition): Seq[String] = {
    checkpointRDD.map(_.getPreferredLocations(split)).getOrElse {
      getPreferredLocations(split)
    }
  }

  /**
    * Internal method to this RDD; will read from cache if applicable, or otherwise compute it.
    * This should ''not'' be called by users directly, but is available for implementors of custom
    * subclasses of RDD.
    */
  final def iterator(split: Partition, context: TaskContext): Iterator[T] = {
    if (storageLevel != StorageLevel.NONE) {
      getOrCompute(split, context)
    } else {
      computeOrReadCheckpoint(split, context)
    }
  }

  /**
    * Return the ancestors of the given RDD that are related to it only through a sequence of
    * narrow dependencies. This traverses the given RDD's dependency tree using DFS, but maintains
    * no ordering on the RDDs returned.
    */
  private[spark] def getNarrowAncestors: Seq[RDD[_]] = {
    val ancestors = new mutable.HashSet[RDD[_]]

    def visit(rdd: RDD[_]) {
      val narrowDependencies = rdd.dependencies.filter(_.isInstanceOf[NarrowDependency[_]])
      val narrowParents = narrowDependencies.map(_.rdd)
      val narrowParentsNotVisited = narrowParents.filterNot(ancestors.contains)
      narrowParentsNotVisited.foreach { parent =>
        ancestors.add(parent)
        visit(parent)
      }
    }

    visit(this)

    // In case there is a cycle, do not include the root itself
    ancestors.filterNot(_ == this).toSeq
  }

  /**
    * Compute an RDD partition or read it from a checkpoint if the RDD is checkpointing.
    */
  private[spark] def computeOrReadCheckpoint(split: Partition, context: TaskContext): Iterator[T] =
  {
    if (isCheckpointedAndMaterialized) {
      firstParent[T].iterator(split, context)
    } else {
      compute(split, context)
    }
  }

  /**
    * Gets or computes an RDD partition. Used by RDD.iterator() when an RDD is cached.
    */
  private[spark] def getOrCompute(partition: Partition, context: TaskContext): Iterator[T] = {
    val blockId = RDDBlockId(id, partition.index)
    var readCachedBlock = true
    // This method is called on executors, so we need call SparkEnv.get instead of sc.env.
    SparkEnv.get.blockManager.getOrElseUpdate(blockId, storageLevel, elementClassTag, () => {
      readCachedBlock = false
      computeOrReadCheckpoint(partition, context)
    }) match {
      case Left(blockResult) =>
        if (readCachedBlock) {
          val existingMetrics = context.taskMetrics().registerInputMetrics(blockResult.readMethod)
          existingMetrics.incBytesReadInternal(blockResult.bytes)
          new InterruptibleIterator[T](context, blockResult.data.asInstanceOf[Iterator[T]]) {
            override def next(): T = {
              existingMetrics.incRecordsReadInternal(1)
              delegate.next()
            }
          }
        } else {
          new InterruptibleIterator(context, blockResult.data.asInstanceOf[Iterator[T]])
        }
      case Right(iter) =>
        new InterruptibleIterator(context, iter.asInstanceOf[Iterator[T]])
    }
  }

  /**
    * Execute a block of code in a scope such that all new RDDs created in this body will
    * be part of the same scope. For more detail, see {{org.apache.spark.rdd.RDDOperationScope}}.
    *
    * Note: Return statements are NOT allowed in the given body.
    */
  private[spark] def withScope[U](body: => U): U = RDDOperationScope.withScope[U](sc)(body)

  // Transformations (return a new RDD)

  /**
    * Return a new RDD by applying a function to all elements of this RDD.
    */
  def map[U: ClassTag](f: T => U): RDD[U] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[U, T](this, (context, pid, iter) => iter.map(cleanF))
  }

  /**
    *  Return a new RDD by first applying a function to all elements of this
    *  RDD, and then flattening the results.
    */
  def flatMap[U: ClassTag](f: T => TraversableOnce[U]): RDD[U] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[U, T](this, (context, pid, iter) => iter.flatMap(cleanF))
  }

  /**
    * Return a new RDD containing only the elements that satisfy a predicate.
    */
  def filter(f: T => Boolean): RDD[T] = withScope {
    val cleanF = sc.clean(f)
    new MapPartitionsRDD[T, T](
      this,
      (context, pid, iter) => iter.filter(cleanF),
      preservesPartitioning = true)
  }

  /**
    * Return a new RDD containing the distinct elements in this RDD.
    */
  def distinct(numPartitions: Int)(implicit ord: Ordering[T] = null): RDD[T] = withScope {
    map(x => (x, null)).reduceByKey((x, y) => x, numPartitions).map(_._1)
  }

  /**
    * Return a new RDD containing the distinct elements in this RDD.
    */
  def distinct(): RDD[T] = withScope {
    distinct(partitions.length)
  }

  /**
    * Return a new RDD that has exactly numPartitions partitions.
    *
    * Can increase or decrease the level of parallelism in this RDD. Internally, this uses
    * a shuffle to redistribute data.
    *
    * If you are decreasing the number of partitions in this RDD, consider using `coalesce`,
    * which can avoid performing a shuffle.
    */
  def repartition(numPartitions: Int)(implicit ord: Ordering[T] = null): RDD[T] = withScope {
    coalesce(numPartitions, shuffle = true)
  }

  /**
    * Return a new RDD that is reduced into `numPartitions` partitions.
    *
    * This results in a narrow dependency, e.g. if you go from 1000 partitions
    * to 100 partitions, there will not be a shuffle, instead each of the 100
    * new partitions will claim 10 of the current partitions.
    *
    * However, if you're doing a drastic coalesce, e.g. to numPartitions = 1,
    * this may result in your computation taking place on fewer nodes than
    * you like (e.g. one node in the case of numPartitions = 1). To avoid this,
    * you can pass shuffle = true. This will add a shuffle step, but means the
    * current upstream partitions will be executed in parallel (per whatever
    * the current partitioning is).
    *
    * Note: With shuffle = true, you can actually coalesce to a larger number
    * of partitions. This is useful if you have a small number of partitions,
    * say 100, potentially with a few partitions being abnormally large. Calling
    * coalesce(1000, shuffle = true) will result in 1000 partitions with the
    * data distributed using a hash partitioner.
    */
  def coalesce(numPartitions: Int, shuffle: Boolean = false)(implicit ord: Ordering[T] = null)
  : RDD[T] = withScope {
    if (shuffle) {
      /** Distributes elements evenly across output partitions, starting from a random partition. */
      val distributePartition = (index: Int, items: Iterator[T]) => {
        var position = (new Random(index)).nextInt(numPartitions)
        items.map { t =>
          // Note that the hash code of the key will just be the key itself. The HashPartitioner
          // will mod it with the number of total partitions.
          position = position + 1
          (position, t)
        }
      } : Iterator[(Int, T)]

      // include a shuffle step so that our upstream tasks are still distributed
      new CoalescedRDD(
        new ShuffledRDD[Int, T, T](mapPartitionsWithIndex(distributePartition),
          new HashPartitioner(numPartitions)),
        numPartitions).values
    } else {
      new CoalescedRDD(this, numPartitions)
    }
  }

  /**
    * Return a sampled subset of this RDD.
    *
    * @param withReplacement can elements be sampled multiple times (replaced when sampled out)
    * @param fraction expected size of the sample as a fraction of this RDD's size
    *  without replacement: probability that each element is chosen; fraction must be [0, 1]
    *  with replacement: expected number of times each element is chosen; fraction must be >= 0
    * @param seed seed for the random number generator
    */
  def sample(
              withReplacement: Boolean,
              fraction: Double,
              seed: Long = Utils.random.nextLong): RDD[T] = withScope {
    require(fraction >= 0.0, "Negative fraction value: " + fraction)
    if (withReplacement) {
      new PartitionwiseSampledRDD[T, T](this, new PoissonSampler[T](fraction), true, seed)
    } else {
      new PartitionwiseSampledRDD[T, T](this, new BernoulliSampler[T](fraction), true, seed)
    }
  }

  /**
    * Randomly splits this RDD with the provided weights.
    *
    * @param weights weights for splits, will be normalized if they don't sum to 1
    * @param seed random seed
    *
    * @return split RDDs in an array
    */
  def randomSplit(
                   weights: Array[Double],
                   seed: Long = Utils.random.nextLong): Array[RDD[T]] = withScope {
    val sum = weights.sum
    val normalizedCumWeights = weights.map(_ / sum).scanLeft(0.0d)(_ + _)
    normalizedCumWeights.sliding(2).map { x =>
      randomSampleWithRange(x(0), x(1), seed)
    }.toArray
  }

  /**
    * Internal method exposed for Random Splits in DataFrames. Samples an RDD given a probability
    * range.
    * @param lb lower bound to use for the Bernoulli sampler
    * @param ub upper bound to use for the Bernoulli sampler
    * @param seed the seed for the Random number generator
    * @return A random sub-sample of the RDD without replacement.
    */
  private[spark] def randomSampleWithRange(lb: Double, ub: Double, seed: Long): RDD[T] = {
    this.mapPartitionsWithIndex( { (index, partition) =>
      val sampler = new BernoulliCellSampler[T](lb, ub)
      sampler.setSeed(seed + index)
      sampler.sample(partition)
    }, preservesPartitioning = true)
  }

  /**
    * Return a fixed-size sampled subset of this RDD in an array
    *
    * @note this method should only be used if the resulting array is expected to be small, as
    * all the data is loaded into the driver's memory.
    *
    * @param withReplacement whether sampling is done with replacement
    * @param num size of the returned sample
    * @param seed seed for the random number generator
    * @return sample of specified size in an array
    */
  def takeSample(
                  withReplacement: Boolean,
                  num: Int,
                  seed: Long = Utils.random.nextLong
                ): Array[T] = withScope {
    val numStDev = 10.0
    require(num >= 0, "Negative number of elements requested.")
    require(
      num <= (Int.MaxValue - (numStDev * math.sqrt(Int.MaxValue)).toInt),
      "Cannot support a sample size > Int.MaxValue - " +
        s"$numStDev * math.sqrt(Int.maxValue")

    if (num == 0) {
      new Array[T](0)
    } else {
      val rand = new Random(seed)
      if (!withReplacement && num >= initalCount) {
        Utils.randomizeInPlace(this.collect(), rand)
      } else {
        val fraction = Samplingutils.computeFractionForSampleSize(num, initialCount,
          withReplacement)
        var samples = this.sample(withReplacement, fraction, rand.nextInt()).collect()

        // If the first sample didn't turn out large enough, keep trying to take samples;
        // this shouldn't happen often because we use a big multiplier for the initial size
        var numIters = 0
        while (samples.length < num) {
          logWarning(s"needed to re-sample due to insufficient sample size. Repeat #$numIters")
          samples = this.sample(withReplacement, fraction, rand.nextInt()).collect()
          numIters += 1
        }
        Utils.randomizeInPlace(samples, rand).take(num)
      }

    }
  }

  /**
    * Return the union of this RDD and another one. Any identical elements will appear mutiple
    * time (use '.distinct()' to eliminate them).
    */
  def union(other: RDD[T]): RDD[T] = withScope {
    if (partitioner.isDefined && other.partitioner == partitioner) {
      new PartitionerAwareUnionRDD(sc, Array(this, other))
    } else {
      new UnionRDD(sc, Array(this, other))
    }
  }

  /**
    * Return the union of this RDD and another one. Any identical elements will appear multiple
    * times (use `.distinct()` to eliminate them
    */
  def ++(other: RDD[T]): RDD[T] = withScope {
    this.union(other)
  }

  /**
    * Return this RDD sorted by the given key function.
    */
  def sortBy[K](
                 f: (T) => K,
                 ascending: Boolean = true,
                 numPartitions: Int = this.partitions.length
               )
               (implicit ord: Ordering[K], ctag: ClassTag[K]): RDD[T] = withScope {
    this.keyBy[K](f).sortByKey(ascending, numPartitions).values
  }

  /**
    * Return the intersection of this RDD and another one. The output will not contain any duplicate
    * elements, even if the input RDDs did.
    *
    * Note that this method performs a shuffle internally
    */
  def intersection(other: RDD[T]): RDD[T] = withScope {
    this.map(v => (v, null)).cogroup(other.map(v => (v, null)))
      .filter { case (_, (leftGroup, rightGroup)) => leftGroup.nonEmpty && rightGroup.nonEmply }
      .keys
  }


  private var storageLevel: StorageLevel = StorageLevel.NONE
}



































