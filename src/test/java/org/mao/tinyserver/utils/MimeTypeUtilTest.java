package org.mao.tinyserver.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class MimeTypeUtilTest {

    @Test
    public void findContentType() {
        String MimeType = MimeTypeUtil.findContentType("index.html");
        System.out.println(MimeType);
    }
}