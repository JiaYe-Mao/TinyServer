package org.mao.tinyserver.io;
/*
 * @(#)ChannelIOSecure.java	1.2 04/07/26
 * 
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduce the above copyright notice, 
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may 
 * be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL 
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST 
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, 
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY 
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, 
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */


import org.mao.tinyserver.utils.BytesUtil;
import org.mao.tinyserver.utils.LoggerUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A helper class which performs I/O using the SSLEngine API.
 * <p>
 * Each connection has a SocketChannel and a SSLEngine that is
 * used through the lifetime of the Channel.  We allocate byte buffers
 * for use as the outbound and inbound network buffers.
 * <p>
 * <PRE>
 * Application Data
 * src      requestBB
 * |           ^
 * |     |     |
 * v     |     |
 * +----+-----|-----+----+
 * |          |          |
 * |       SSL|Engine    |
 * wrap()  |          |          |  unwrap()
 * | OUTBOUND | INBOUND  |
 * |          |          |
 * +----+-----|-----+----+
 * |     |     ^
 * |     |     |
 * v           |
 * outNetBB     inNetBB
 * Net data
 * </PRE>
 * <p>
 * These buffers handle all of the intermediary data for the SSL
 * connection.  To make things easy, we'll require outNetBB be
 * completely flushed before trying to wrap any more data, but we
 * could certainly remove that restriction by using larger buffers.
 * <p>
 * There are many, many ways to handle compute and I/O strategies.
 * What follows is a relatively simple one.  The reader is encouraged
 * to develop the strategy that best fits the application.
 * <p>
 * In most of the non-blocking operations in this class, we let the
 * Selector tell us when we're ready to attempt an I/O operation (by the
 * application repeatedly calling our methods).  Another option would be
 * to attempt the operation and return from the method when no forward
 * progress can be made.
 * <p>
 * There's lots of room for enhancements and improvement in this example.
 * <p>
 * We're checking for SSL/TLS end-of-stream truncation attacks via
 * sslEngine.closeInbound().  When you reach the end of a input stream
 * via a read() returning -1 or an IOException, we call
 * sslEngine.closeInbound() to signal to the sslEngine that no more
 * input will be available.  If the peer's close_notify message has not
 * yet been received, this could indicate a trucation attack, in which
 * an attacker is trying to prematurely close the connection.   The
 * closeInbound() will throw an exception if this condition were
 * present.
 *
 * @author Brad R. Wetmore
 * @author Mark Reinhold
 * @version 1.2, 04/07/26
 */
public class SSLReadWriteSelectorHandler extends PlainReadWriteSelectorHandler {

    private static final Logger LOGGER = LoggerUtil.getLogger(SSLReadWriteSelectorHandler.class);
    /*
     * An empty ByteBuffer for use when one isn't available, say
     * as a source buffer during initial handshake wraps or for close
     * operations.
     */
    private static ByteBuffer hsBB = ByteBuffer.allocate(0);
    private SSLEngine sslEngine = null;
    private CharsetDecoder decoder = Charset.forName("UTF8").newDecoder();

    /*
     * All I/O goes through these buffers.
     * <P>
     * It might be nice to use a cache of ByteBuffers so we're
     * not alloc/dealloc'ing ByteBuffer's for each new SSLEngine.
     * <P>
     * We use our superclass' requestBB for our application input buffer.
     * Outbound application data is supplied to us by our callers.
     */
    private ByteBuffer inNetBB;
    private ByteBuffer outNetBB;
    private ByteBuffer appIn;
    private ByteBuffer appOut;
    /*
     * The FileChannel we're currently transferTo'ing (reading).
     */
    private ByteBuffer fileChannelBB = null;
    private boolean handshakeDone = false;

    /*
     * During our initial handshake, keep track of the next
     * SSLEngine operation that needs to occur:
     *
     *     NEED_WRAP/NEED_UNWRAP
     *
     * Once the initial handshake has completed, we can short circuit
     * handshake checks with initialHSComplete.
     */
    //private HandshakeStatus initialHSStatus;
    //private boolean initialHSComplete;

    /*
     * We have received the shutdown request by our caller, and have
     * closed our outbound side.
     */
    private boolean shutdown = false;

    private Selector selector;
    private SelectionKey key;

    /*
     * Constructor for a secure ChannelIO variant.
     */
    public SSLReadWriteSelectorHandler(SocketChannel sc, SelectionKey selectionKey,
                                       SSLContext sslContext, Selector selector) throws IOException {
        super(sc);

        sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(false);
        //initialHSStatus = HandshakeStatus.NEED_UNWRAP;

        this.selector = selector;
        this.key = selectionKey;

        int netBBSize = sslEngine.getSession().getPacketBufferSize();
        inNetBB = ByteBuffer.allocate(netBBSize);
        outNetBB = ByteBuffer.allocate(netBBSize);
        outNetBB.position(0);
        outNetBB.limit(0);
        appIn = ByteBuffer.allocate(sslEngine.getSession().getApplicationBufferSize() + 10);
        appOut = ByteBuffer.allocate(netBBSize);

        int appBBSize = sslEngine.getSession().getApplicationBufferSize();
        requestBB = ByteBuffer.allocate(appBBSize);

        doHandshake(selectionKey);
    }

    /*
     * Writes bb to the SocketChannel.
     * <P>
     * Returns true when the ByteBuffer has no remaining data.
     */
    private boolean tryFlush(ByteBuffer bb) throws IOException {
        sc.write(bb);
        return !bb.hasRemaining();
    }

    /*
     * Perform any handshaking processing.
     * <P>
     * If a SelectionKey is passed, register for selectable
     * operations.
     * <P>
     * In the blocking case, our caller will keep calling us until
     * we finish the handshake.  Our reads/writes will block as expected.
     * <P>
     * In the non-blocking case, we just received the selection notification
     * that this channel is ready for whatever the operation is, so give
     * it a try.
     * <P>
     * return:
     *		true when handshake is done.
     *		false while handshake is in progress
     */
    boolean doHandshake(SelectionKey sk) throws IOException {

        sslEngine.beginHandshake();//explicitly begin the handshake
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        while (!handshakeDone) {
            switch (hsStatus) {
                case FINISHED:
                    //the status become FINISHED only when the ssl handshake is finished
                    //but we still need to send data, so do nothing here
                    break;
                case NEED_TASK:
                    //do the delegate task if there is some extra work such as checking the keystore during the handshake
                    hsStatus = doTask();
                    break;
                case NEED_UNWRAP:
                    //unwrap means unwrap the ssl packet to get ssl handshake information
                    sc.read(inNetBB);
                    inNetBB.flip();
                    hsStatus = doUnwrap();
                    break;
                case NEED_WRAP:
                    //wrap means wrap the app packet into an ssl packet to add ssl handshake information
                    hsStatus = doWrap();
                    sc.write(outNetBB);
                    outNetBB.clear();
                    break;
                case NOT_HANDSHAKING:
                    //now it is not in a handshake or say byebye status. here it means handshake is over and ready for ssl talk
                    sc.configureBlocking(false);//set the socket to unblocking mode
                    sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);//register the read and write event
                    handshakeDone = true;
                    break;
            }
        }
        return true;
    }

    private HandshakeStatus doTask() {
        Runnable runnable;
        while ((runnable = sslEngine.getDelegatedTask()) != null)
        {
            System.out.println("\trunning delegated task...");
            runnable.run();
        }
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        if (hsStatus == HandshakeStatus.NEED_TASK)
        {
            //throw new Exception("handshake shouldn't need additional tasks");
            System.out.println("handshake shouldn't need additional tasks");
        }
        System.out.println("\tnew HandshakeStatus: " + hsStatus);

        return hsStatus;
    }

    private HandshakeStatus doUnwrap() throws SSLException{
        HandshakeStatus hsStatus;
        do{//do unwrap until the state is change to "NEED_WRAP"
            SSLEngineResult engineResult = sslEngine.unwrap(inNetBB, appIn);
            //log("server unwrap: ", engineResult);
            hsStatus = doTask();
        }while(hsStatus ==  SSLEngineResult.HandshakeStatus.NEED_UNWRAP && inNetBB.remaining()>0);
        System.out.println("\tnew HandshakeStatus: " + hsStatus);
        inNetBB.clear();
        return hsStatus;
    }

    private HandshakeStatus doWrap() throws SSLException{
        HandshakeStatus hsStatus;
        SSLEngineResult engineResult = sslEngine.wrap(appOut, outNetBB);
        //log("server wrap: ", engineResult);
        hsStatus = doTask();
        System.out.println("\tnew HandshakeStatus: " + hsStatus);
        outNetBB.flip();
        return hsStatus;
    }



    /*
     * Do all the outstanding handshake tasks in the current Thread.
     */
    private HandshakeStatus doTasks() {

        Runnable runnable;

	/*
     * We could run this in a separate thread, but
	 * do in the current for now.
	 */
        while ((runnable = sslEngine.getDelegatedTask()) != null) {
            runnable.run();
        }
        return sslEngine.getHandshakeStatus();
    }



    /*
     * Try to flush out any existing outbound data, then try to wrap
     * anything new contained in the src buffer.
     * <P>
     * Return the number of bytes actually consumed from the buffer,
     * but the data may actually be still sitting in the output buffer,
     * waiting to be flushed.
     */
    private int doWrite(ByteBuffer src) throws IOException {
        int retValue = 0;

        if (outNetBB.hasRemaining() && !tryFlush(outNetBB)) {
            return retValue;
        }

	/*
     * The data buffer is empty, we can reuse the entire buffer.
	 */
        outNetBB.clear();

        SSLEngineResult result = sslEngine.wrap(src, outNetBB);
        retValue = result.bytesConsumed();

        outNetBB.flip();

        switch (result.getStatus()) {

            case OK:
                if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                    doTasks();
                }
                break;

            default:
                throw new IOException("sslEngine error during data write: " +
                        result.getStatus());
        }

	/*
     * Try to flush the data, regardless of whether or not
	 * it's been selected.  Odds of a write buffer being full
	 * is less than a read buffer being empty.
	 */
        tryFlush(src);
        if (outNetBB.hasRemaining()) {
            tryFlush(outNetBB);
        }

        return retValue;
    }



    @Override
    public void handleWrite(ByteBuffer byteBuffer) throws IOException {
        byteBuffer.flip();
        SSLEngineResult engineResult = sslEngine.wrap(byteBuffer, outNetBB);
        //log("server wrap: ", engineResult);
        doTask();
        //runDelegatedTasks(engineResult, sslEngine);
        if (engineResult.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING)
        {
            System.out.println("text sent");
        }
        outNetBB.flip();
        sc.write(outNetBB);
        outNetBB.compact();
    }

    @Override
    public ByteBuffer handleRead() throws IOException {
        return read();
    }

    /*
     * Read the channel for more information, then unwrap the
     * (hopefully application) data we get.
     * <P>
     * If we run out of data, we'll return to our caller (possibly using
     * a Selector) to get notification that more is available.
     * <P>
     * Each call to this method will perform at most one underlying read().
     */
    ByteBuffer read() throws IOException {
        //SSLEngineResult result;

        if (!handshakeDone) {
            throw new IllegalStateException();
        }

        if (sslEngine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING)
        {
            sc.read(inNetBB);
            inNetBB.flip();

            SSLEngineResult engineResult = sslEngine.unwrap(inNetBB, appIn);
            //log("server unwrap: ", engineResult);
            doTask();
            //runDelegatedTasks(engineResult, sslEngine);
            inNetBB.compact();
            if (engineResult.getStatus() == SSLEngineResult.Status.OK)
            {
                System.out.println("text recieved");
                appIn.flip();// ready for reading
                System.out.println(decoder.decode(appIn));
                appIn.compact();
            }
            else if(engineResult.getStatus() == SSLEngineResult.Status.CLOSED) {
                doSSLClose(key);
            }

        }
        return appIn;
    }

    @Override
    public void close() {
        try {
            while (!dataFlush()) {

            }
            do {
            } while (!shutdown());
            super.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }

    /*
     * Begin the shutdown process.
     * <P>
     * Close out the SSLEngine if not already done so, then
     * wrap our outgoing close_notify message and try to send it on.
     * <P>
     * Return true when we're done passing the shutdown messsages.
     */
    boolean shutdown() throws IOException {

        if (!shutdown) {
            sslEngine.closeOutbound();
            shutdown = true;
        }

        if (outNetBB.hasRemaining() && tryFlush(outNetBB)) {
            return false;
        }

        /*
         * By RFC 2616, we can "fire and forget" our close_notify
         * message, so that's what we'll do here.
         */
        outNetBB.clear();
        SSLEngineResult result = sslEngine.wrap(hsBB, outNetBB);
        if (result.getStatus() != Status.CLOSED) {
            throw new SSLException("Improper close state");
        }
        outNetBB.flip();

        /*
         * We won't wait for a select here, but if this doesn't work,
         * we'll cycle back through on the next select.
         */
        if (outNetBB.hasRemaining()) {
            tryFlush(outNetBB);
        }

        return (!outNetBB.hasRemaining() &&
                (result.getHandshakeStatus() != HandshakeStatus.NEED_WRAP));
    }

    /*
     * Flush any remaining data.
     * <P>
     * Return true when the fileChannelBB and outNetBB are empty.
     */
    boolean dataFlush() throws IOException {
        boolean fileFlushed = true;

        if ((fileChannelBB != null) && fileChannelBB.hasRemaining()) {
            doWrite(fileChannelBB);
            fileFlushed = !fileChannelBB.hasRemaining();
        } else if (outNetBB.hasRemaining()) {
            tryFlush(outNetBB);
        }

        return (fileFlushed && !outNetBB.hasRemaining());
    }

    //close an ssl talk, similar to the handshake steps
    private void doSSLClose(SelectionKey key) throws IOException {
        key.cancel();
        try
        {
            sc.configureBlocking(true);
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        HandshakeStatus hsStatus = sslEngine.getHandshakeStatus();
        while(handshakeDone) {
            switch(hsStatus) {
                case FINISHED:

                    break;
                case NEED_TASK:
                    hsStatus = doTask();
                    break;
                case NEED_UNWRAP:
                    sc.read(inNetBB);
                    inNetBB.flip();
                    hsStatus = doUnwrap();
                    break;
                case NEED_WRAP:
                    hsStatus = doWrap();
                    sc.write(outNetBB);
                    outNetBB.clear();
                    break;
                case NOT_HANDSHAKING:
                    handshakeDone = false;
                    sc.close();
                    break;
            }
        }
    }
}
