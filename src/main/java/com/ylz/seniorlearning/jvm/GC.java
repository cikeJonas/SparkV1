package com.ylz.seniorlearning.jvm;

/**
 * 引用计数算法:
 * 缺点:不能解决互相引用问题.
 */
public class GC {
    public static final int oneM = 1024 * 1024;
    public Object instance = null;
    private byte[] bytes = new byte[2 * oneM];

    public static void testGC() {
        GC objA = new GC();
        GC objB = new GC();
        objA.instance = objB;
        objB.instance = objA;

        objA = null;
        objB = null;
        System.gc();
    }

    public static void testGCDetail() {

        byte[] allocation1,allocation2,allocation3,allocation4;
        allocation1 = new byte[2*1024*1024];
        allocation2 = new byte[2*1024*1024];
         allocation3= new byte[2*1024*1024];
        allocation4= new byte[4*1024*1024];
    }

    /**
     * Vm args : -Xms20m -Xmx20m -Xmn10M -XX:SurvivorRatio=8 -XX:PrintGCDetails
     * @param args
     */
    public static void main(String[] args){
        //testGC();
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
