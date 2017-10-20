package com.ylz.senior.learning.algorithm;

import org.junit.Test;

/**
 * Created by Jonas on 2017/9/22.
 */
public class NoteTree {
    private int loc(int data, int[] a,int zstart, int zend) {
        for (int i = zstart; i < zend; i++) {
            if (data == a[i])
                return i;
        }
        return -1;
    }

    /**
     * 根据先序和中序重建二叉树
     */
    private TNote buildTree(int[] xianxu, int start, int end, int[] zhongxu, int zstart, int zend) {
        if (xianxu.length <= 0 || start > end)
            return null;
        if (zhongxu.length <= 0 || zstart > zend)
            return null;
        TNote note = new TNote();
        note.data = xianxu[start];
        note.leftNote = null;
        note.rightNote = null;
        int index = loc(note.data, zhongxu, zstart, zend);
        int diff = index - zstart - 1;
        note.leftNote = buildTree(xianxu, start+1, start + diff+1, zhongxu, zstart, zstart+diff);
       // note.rightNote = buildTree(xianxu, start+diff+2,end,zhongxu,index+1,zend);
        return note;

    }



    @Test
    public void testBuildTree() {
        int[] xianxu = {7, 10, 4, 3, 1, 2, 8, 11};
        int[] zhongxu = {4, 10, 3, 1, 7, 11, 8, 2};
        TNote note = buildTree(xianxu, 0, xianxu.length - 1, zhongxu, 0, zhongxu.length - 1);
        printTree(note);

    }

    private void printTree(TNote note) {
        if (note.leftNote !=null) {
            System.out.print(note.data+" ");
            printTree(note.leftNote);
        }
        if (note.rightNote != null) {
            System.out.print(note.data + " ");
            printTree(note.rightNote);
        }

    }

    static class TNote {
        public TNote getLeftNote() {
            return leftNote;
        }

        public TNote getRightNote() {
            return rightNote;
        }

        public int getData() {
            return data;
        }

        TNote leftNote;
        TNote rightNote;
        int data;

    }



}
