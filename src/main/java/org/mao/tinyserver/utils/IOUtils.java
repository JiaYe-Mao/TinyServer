package org.mao.tinyserver.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class IOUtils {

    // 只能用于本地, 不能用于socket, 因为available()获得的长度不一定是真实长度.
    public static ByteBuffer convertInputStreamIntoByteBuffer(InputStream inputStream){
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(inputStream.available());
            byte[] tmp = new byte[1024];
            int length;
            while((length = inputStream.read(tmp)) != -1){
                byteBuffer.put(tmp, 0, length);
            }


            return byteBuffer;
        } catch (IOException e) {
            // ignored (Happened only if inputStream was closed.)
        }

        return null;
    }
}
