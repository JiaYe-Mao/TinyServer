package org.mao.tinyserver.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class PathUtilTest {

    @Test
    public void getRootPath() {
        // jar包所在位置
        System.out.println(PathUtil.getRootPath());

    }

    @Test
    public void getTempPath(){
        System.out.println(PathUtil.getTempPath());
    }
}