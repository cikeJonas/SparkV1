package org.apache.spark.v1

import org.apache.spark._
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.rdd.CheckpointRDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.Utils

import scala.reflect.ClassTag

abstract class RDD[T: ClassTag](
                                 @transient private var _sc: SparkContext,
                                 @transient private var deps: Seq[Dependency[_]]

                               ) extends Serializable with Logging {
  if (classOf[RDD[_]].isAssignableFrom(elementClassTag.runtimeClass)) {
    //This is a warning instead of an exception in order to avoid breaking user programs that
    //might have defined nested RDDs without running jobs with them
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

  //=================================================================

  //Methods that should be implemented by subclasses of RDD

  //=================================================================

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


  //===================================================================
  // Methods and fields available on all RDDs

  //===================================================================

  /**
    * The SparkContext thar created this RDDs
    */
  def sparkContext: SparkContext = sc

  /** A unique ID for this RDD (within its SparkContext). */
  val id: Int = sc.newRddId()
  /** A friendly name for this RDD */
  @transient var name: String = null

  //Assign a name to this RDD
  def setName(_name: String): this.type = {
    name = _name
    this
  }

  /**
    * Mark this RDD for persisting using the specified level.
    *
    *
    */
  private def persist(newLevel: StorageLevel, allowOverride: Boolean): this.type = {
    //TODO : Handle changes of StorageLevel
    if (storageLevel != StorageLevel.NONE && newLevel != storageLevel && !allowOverride) {
      throw new UnsupportedOperationException(
        "Cannot change storage level of an RDD after it was already assigned a level"
      )
    }

    //If this is the first time this RDD is marked for persisting, register it
    //with the SparkContext for cleanups and accounting. Do this only once.
    if (storageLevel == StorageLevel.NONE) {
      sc.cleaner.foreach(_.registerRDDforCleanup(this))
      sc.persistRdd(this)
    }

    storageLevel = newLevel
    this
  }

  /**
    * Set this RDD's storage level to persist its values across operations after the first time
    * it is computed.This can only be used to assign a new Storage level if the RDD does not have
    * a storage level set yet.Local checkpointing is an exception.
    */

  def persist(newLevel: StorageLevel): this.type = {
    if (isLocallyCheckpointed) {
      //This means the user previously called localCheckpoint(), which should have already
      //marked this RDD for persisting. Here we should override the old storage level whit
      //ont thate is explicitly requested by the user (after adapting it to use disk).
      persist(LocalRddCheckpointData.transformStorageLevel(newLevel), allowOverride = true)
    } else {
      persist(newLevel, allowOverride = false)
    }

  }

  /**
    * Persist this RDD with the default storage level ('MEMORY_ONLY').*/

  def persist(): this.type = persist(StorageLevel.MEMORY_ONLY)

  //Persist this RDD with the default storage level ('MEMORY_ONLY').
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

  //Our dependencies and partitions will be gotten by calling subclass's methods below, and will
  //be overwritten when we're checkpointed
  private var dependencies_ : Seq[Dependency[_]] = null
  @transient private var partitions_ : Array[Partition] = null

  /** An Option holding our checkpoint RDD, if we are checkpointed */
  private def checkpointRDD: Option[CheckpointRDD[T]] = checkpointRDD.flatMap(_.checkpointRDD)

  /**
    * Get the list of dependencies of this RDD, taking into account whether the
    * RDD if checkpoined or not.
    **/
  final def dependencies: Seq[Dependency[_]] = {
    checkpointRDD.map(r => List(new OneToOneDependency(r))).getOrElse {
      if (dependencies_ == null) {
        dependencies_ = getDependencies
      }
      dependencies_
    }
  }

  def takeSample(withReplacement:
    Boolean, num: Int, seed: Long = Utils.random.nextLong
                )

  private var storageLevel: StorageLevel = StorageLevel.NONE
}



































