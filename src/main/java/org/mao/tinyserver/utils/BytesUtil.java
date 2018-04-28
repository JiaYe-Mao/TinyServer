package org.mao.tinyserver.utils;


import org.mao.tinyserver.exception.IllegalRequestException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BytesUtil {

    private static final Logger LOGGER = LoggerUtil.getLogger(BytesUtil.class);
    private static final int BUFFER_SIZE = 1024;

    public static byte[] mergeBytes(byte[]... bytes) {
        int bytesSize = 0;
        for (byte[] bs : bytes) {
            bytesSize += bs.length;
        }
        byte nBytes[] = new byte[bytesSize];
        int size = 0;
        for (byte[] bs : bytes) {
            System.arraycopy(bs, 0, nBytes, size, bs.length);
            size += bs.length;
        }
        return nBytes;
    }

    public static byte[] subBytes(byte[] b, int start, int length) {
        byte bytes[] = new byte[length];
        System.arraycopy(b, start, bytes, 0, length);
        return bytes;
    }

    public static byte[] getBytesFromChannel(SocketChannel sc, int maxBytesSize) throws IllegalRequestException {

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] tempByte = new byte[BUFFER_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try{
            int length;
            int bytesSize = 0;
            while (sc.read(buffer) != 0){
                buffer.flip();
                length = buffer.remaining();
                buffer.get(tempByte, 0, length);
                bout.write(tempByte, 0, length);
                buffer.clear();
                bytesSize++;
                if (bytesSize > maxBytesSize){
                    throw new IllegalRequestException("The Content-Length outside the max upload size " + maxBytesSize);
                }
            }
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "", e);
        }
        return bout.toByteArray();
    }


    public static void writeBytesToFile(byte[] bytes, File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(bytes);
            out.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }


}
