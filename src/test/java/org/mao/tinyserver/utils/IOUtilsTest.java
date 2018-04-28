package org.mao.tinyserver.utils;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class IOUtilsTest {


    @Test
    public void convertInputStreamIntoByteBuffer() {

        InputStream inputStream = IOUtilsTest.class.getResourceAsStream("/html/index.html");

        ByteBuffer byteBuffer = IOUtils.convertInputStreamIntoByteBuffer(inputStream);
        System.out.println(byteBuffer);
    }
}