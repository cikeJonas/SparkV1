package com.ylz.seniorlearning.quartz;

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
        logger.info("run this each time");
    }

    public void test() {
        logger.info("running");
    }
}
