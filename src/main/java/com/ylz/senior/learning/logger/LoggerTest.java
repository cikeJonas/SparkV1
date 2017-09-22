package com.ylz.senior.learning.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There has mutil logger, log4j, slf4j. difference between them:
 * 1.slf4j can decrease strings by using {}
 *
 */
public class LoggerTest {
    private final Logger logger = LoggerFactory.getLogger(LoggerTest.class);

    private void testLog() {
        logger.info("test looooo {}", "fdsafewasf");
    }
}
