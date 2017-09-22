package com.ylz.senior.learning.jvm;

/**
 * 引用计数算法:
 * 缺点:不能解决互相引用问题.实际不会使用此算法
 * 可达性分析算法.
 * <p>
 * gc log
 * 1.jps
 * 2.jinfo
 * 3.jmap
 * 4.jstack 线程具体信息
 * 可视化工具
 * 5.jconsole
 * 6.virtualVm
 * 7.BTrace
 * 8.memory analysis tool
 */
public class GC {
    public static final int ONE_M = 1024 * 1024;

    public static void testGCDetail() {
        byte[] allocation1, allocation2, allocation3, allocation4;
        allocation1 = new byte[2 * ONE_M];
        allocation2 = new byte[2 * ONE_M];
        allocation3 = new byte[2 * ONE_M];
        allocation4 = new byte[2 * 2 * ONE_M];
        System.out.println(allocation1.toString() + allocation2.toString() + allocation3.toString() + allocation4.toString());
    }

    /**
     * Vm args : -Xms20m -Xmx20m -Xmn10M -XX:SurvivorRatio=8 -XX:PrintGCDetails
     *
     * @param args
     */
    public static void main(String[] args) {
        testGCDetail();
    }

    /**
     * 可达性分析算法
     * 优点:
     * 1.解决互相引用问题
     */
    private void reachabilityAnalysis() {
        /**
         * gc root 包括:
         * 1.虚拟机栈中引用的对象
         * 2.方法区中类静态属性引用的对象
         * 3.方法区中常量引用的对象
         * 4.本地方法栈中JNI(本地方法)引用的对象
         */

        /**
         * 引用类型
         1.强引用  new Object()
         2.软引用  在系统发生溢出前,将其回收
         3.弱引用  只能生存一次,在第二次gc必定回收
         4.虚引用  只是用于标记
         */
    }
}
