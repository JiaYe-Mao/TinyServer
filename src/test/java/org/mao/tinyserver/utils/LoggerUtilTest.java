package org.mao.tinyserver.utils;

import org.junit.Test;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class LoggerUtilTest {

    @Test
    public void getLogger() {
        Logger logger = LoggerUtil.getLogger(this.getClass());
        for (int i = 0; i < 5; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.log(Level.SEVERE, "aa");
                }
            }).start();
        }
    }

    @Test
    public void recordStackTraceMsg() {
        Logger logger = LoggerUtil.getLogger(this.getClass());
        logger.log(Level.SEVERE, LoggerUtil.recordStackTraceMsg(new Exception()));
    }
}