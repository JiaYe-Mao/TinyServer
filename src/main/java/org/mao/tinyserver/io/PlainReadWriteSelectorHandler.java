package org.mao.tinyserver.io;


import org.mao.tinyserver.utils.BytesUtil;
import org.mao.tinyserver.utils.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlainReadWriteSelectorHandler implements ReadWriteSelectorHandler {

    private static final Logger LOGGER = LoggerUtil.getLogger(PlainReadWriteSelectorHandler.class);
    private static final Integer BUFFER_SIZE = 1024;

    protected ByteBuffer requestBB;
    protected SocketChannel sc;

    public PlainReadWriteSelectorHandler(SocketChannel sc) {
        this.sc = sc;
        this.requestBB = ByteBuffer.allocate(BUFFER_SIZE);
    }

    @Override
    public void handleWrite(ByteBuffer byteBuffer) throws IOException {
        byteBuffer.flip();
        // Write()方法无法保证能写多少字节到SocketChannel。所以，我们重复调用write()直到Buffer没有要写的字节为止。
        while (byteBuffer.hasRemaining() && sc.isOpen()) {
            int len = sc.write(byteBuffer);
            if (len < 0) {
                throw new EOFException();
            }
        }
    }

    @Override
    public ByteBuffer handleRead() throws IOException {
        //int length = sc.read(requestBB);

        int length = 0;
        List<ByteBuffer> bbList = new ArrayList<>();
        while(true){
            ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
            int partLen = 0;
            if ((partLen = sc.read(bb)) == -1){
                break;
            }
            length += partLen;
            if (partLen != 1024){
                ByteBuffer byteBuffer = ByteBuffer.allocate(partLen);
                byteBuffer.put(BytesUtil.subBytes(bb.array(),0,partLen));
                bbList.add(byteBuffer);
                break;
            }
            bbList.add(bb);

        }
        if (length == 0){
            close();
            throw new EOFException();
        }
        //compute whole length of ByteBuffer
        ByteBuffer requestBB = ByteBuffer.allocate(length);
        for (ByteBuffer bb : bbList){
            requestBB.put(bb.array());
        }
        return requestBB;


//        if (length != -1) {
//            ByteBuffer byteBuffer = ByteBuffer.allocate(length);
//            byteBuffer.put(BytesUtil.subBytes(requestBB.array(), 0, length));
//            resizeRequestBB(length);
//            return byteBuffer;
//        }
//        close();
//        throw new EOFException();
    }

    protected void resizeRequestBB(int remaining) {
        if (requestBB.remaining() < remaining) {
            // Expand buffer for large request
            requestBB = ByteBuffer.allocate(requestBB.capacity() * 2);
        } else {
            requestBB = ByteBuffer.allocate(requestBB.capacity());
        }
    }

    public void close() {
        try {
            sc.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "close SocketChannel", e);
        }
    }

    public SocketChannel getChannel() {
        return sc;
    }
}
