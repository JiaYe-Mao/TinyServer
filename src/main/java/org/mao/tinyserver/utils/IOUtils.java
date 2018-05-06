package org.mao.tinyserver.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IOUtils {
    private static final Logger LOGGER = LoggerUtil.getLogger(IOUtils.class);

    // 只能用于本地, 不能用于socket, 因为available()获得的长度不一定是真实长度.
    public static ByteBuffer convertInputStreamIntoByteBuffer(InputStream inputStream) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(inputStream.available());
            byte[] tmp = new byte[1024];
            int length;
            while ((length = inputStream.read(tmp)) != -1) {
                byteBuffer.put(tmp, 0, length);
            }


            return byteBuffer;
        } catch (IOException e) {
            // ignored (Happened only if inputStream was closed.)
        }

        return null;
    }

    public static byte[] getByteByInputStream(InputStream in) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] tempByte = new byte[1024];
        try {
            int length;
            while ((length = in.read(tempByte)) != -1) {
                bout.write(tempByte, 0, length);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }
        return bout.toByteArray();
    }
}