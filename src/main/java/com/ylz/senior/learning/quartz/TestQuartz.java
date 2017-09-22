package com.ylz.senior.learning.quartz;

import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Created by Jonas on 2017/8/30.
 */
public class TestQuartz extends QuartzJobBean {

    final Logger logger = LoggerFactory.getLogger(TestQuartz.class);

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("run this each five minutes");
        final int[] num = {12, 2, 32, 15, 6, 231};
/*        int start = 0;
        int end = num[num.length];
        while (start <= end) {
            if (num[start] > num[end]) {
                swap(num, start, end);
                start++;
            } else {
                end--;
            }
        }*/
    }

    private int[] swap(int[] num, int i, int j) {
        int tem = num[i];
        num[i] = num[j];
        num[j] = tem;
        return num;
    }

    @Test
    public void testQuickSort() {
        final int[] num = {12, 2, 32, 15, 6, 231, 142};
        int start = 0;
        int min = num[0];
        int end = num.length - 1;
        while (start <= end) {
            while (num[start] < min)
                start++;
            while (num[end] > min)
                end--;
            swap(num, start, end);
        }

        System.out.println(num);
    }


    public void test() {
        logger.info("running");
    }
}
