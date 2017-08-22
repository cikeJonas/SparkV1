package com.ylz.seniorlearning.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jonas on 2017/8/21.
 */
public class JVMOOMTest {
    public static void main(String[] args) {
        /**
         * Vm args : -Xms2m -Xmx2m -XX:+HeapDumpOnOutOfMemoryError
         */
        testOOM();
        return;

    }

    public static void testOOM() {
        int i = 0;
        List<OOMObject> oomObjects = new ArrayList<OOMObject>();
        while (true) {
            oomObjects.add(new OOMObject());
            //System.out.println(i++);
        }
    }

    static class OOMObject {

    }

}
