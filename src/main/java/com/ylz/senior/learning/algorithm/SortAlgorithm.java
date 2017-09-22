package com.ylz.senior.learning.algorithm;

import org.junit.Test;

/**
 * Created by Jonas on 2017/9/21.
 */
public class SortAlgorithm {
    private final int[] a = {1, 23, 100, 2, 32, 321, 45, 65, 74};

    private void printAll(int[] a) {
        for (int i = 0; i < a.length; i++) {
            System.out.print(a[i] + " ");

        }
        System.out.println();
    }

    private void swap(int[] a, int i, int j) {
        int tem = a[i];
        a[i] = a[j];
        a[j] = tem;
    }

    private int partition(int[] a, int low, int high) {
        int privotKey = a[low];
        while (low < high) {
            while (low < high && a[high] >= privotKey) --high;
            swap(a, low, high);
            while (low < high && a[low] <= privotKey) ++low;
            swap(a, low, high);
        }
        printAll(a);
        return low;
    }

    /**
     * 时间复杂度: O(nlog2n)
     */
    private void quickSort(int[] a, int low, int high) {
        if (low < high) {
            int privotLoc = partition(a, low, high);
            quickSort(a, low, privotLoc - 1);
            quickSort(a, privotLoc + 1, high);
        }
    }

    @Test
    public void testQuickSort() {

        printAll(a);
        quickSort(a, 0, a.length - 1);
    }

    /**
     * 稳定
     *
     * @param a
     */
    private void insertSort(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i] < a[i - 1]) {
                int j = i - 1;
                int x = a[i];
                a[i] = a[i - 1];
                while (x < a[j]) {
                    a[j + 1] = a[j];
                    j--;
                }
                a[j + 1] = x;
            }
            printAll(a);
        }
    }

    @Test
    public void testInsertSort() {
        insertSort(a);
    }

    private void shellInsertSort(int[] a, int dk) {
        for (int i = dk; i < a.length; ++i) {
            if (a[i] < a[i - dk]) {
                int j = i - dk;
                int x = a[i];
                a[i] = a[i - dk];
                while (x < a[j]) {
                    a[j + dk] = a[j];
                    j -= dk;
                }
                a[j + dk] = x;
            }
            printAll(a);
        }
    }

    /**
     * 不稳定
     *
     * @param a
     */
    private void shellSort(int[] a) {
        int dk = a.length / 2;
        while (dk >= 1) {
            shellInsertSort(a, dk);
            dk = dk / 2;
        }
    }

    @Test
    public void testShellSort() {
        shellSort(a);
        printAll(a);
    }

    private int minIndex(int[] a, int start) {
        int index = start;
        int min = a[start];
        for (int i = start; i < a.length; i++) {
            if (min > a[i]) {
                index = i;
                min = a[i];
            }

        }
        return index;
    }

    private int maxIndex(int[] a, int start) {
        int index = start - 1;
        int max = a[start - 1];
        for (int i = index; i > 0; i--) {
            if (max < a[i]) {
                index = i;
                max = a[i];
            }
        }
        return index;
    }

    /*
        n*n
     */
    private void selectSort(int[] a) {
        int index;
        for (int i = 0; i < a.length; i++) {
            index = minIndex(a, i);
            swap(a, i, index);
        }
    }

    private void betterSelectSort(int[] a) {
        int minIndex;
        int maxIndex;
        for (int i = 0; i < a.length; i++) {
            minIndex = minIndex(a, i);
            maxIndex = maxIndex(a, a.length - i);
            swap(a, i, minIndex);
            swap(a, a.length - 1 - i, maxIndex);
            printAll(a);
        }
    }

    @Test
    public void testSelectSort() {
        selectSort(a);
        printAll(a);
    }

    @Test
    public void testBetterSelectSort() {
        betterSelectSort(a);
        printAll(a);
    }

    private void headAdjust(int[] h, int s, int length) {
        int temp = h[s];
        int child = 2 * s + 1; //left child
        while (child < length) {
            if (child + 1 < length && h[child] < h[child + 1]) { //right child
                ++child;
            }

            if (h[s] < h[child]) {
                h[s] = h[child];
                s = child;
                child = 2 * s + 1;
            } else {
                break;
            }
            h[s] = temp;
        }

        printAll(a);
    }

    private void buildingHeap(int[] h, int length) {
        for (int i = (length - 1) / 2; i >= 0; --i) {
            headAdjust(h, i, length);
        }
    }

    /**
     * 堆排序
     * 深度为k k=log2n +1    O(nlogn)
     *
     * @param h
     * @param length
     */
    private void heapSort(int[] h, int length) {
        buildingHeap(h, length);
        for (int i = length - 1; i > 0; --i) {
            swap(h, i, 0);
            headAdjust(h, 0, i);
        }
    }

    @Test
    public void testHeapSort() {
        heapSort(a, a.length);
        printAll(a);
    }

    private void bubbleSort(int[] a, int length) {
        for (int i = 0; i < length - 1; i++) {
            for (int j = i; j < length - 1 - i; j++) {
                if (a[j] > a[j + 1])
                    swap(a, j, j + 1);
            }
        }
    }


    @Test
    public void testBubbleSort() {
        bubbleSort(a, a.length);
        printAll(a);
    }

    
}
