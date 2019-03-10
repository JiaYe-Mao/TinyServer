package org.mao.tinyserver.io;

import org.mao.tinyserver.config.ServerConfig;
import org.mao.tinyserver.config.ServerConfigBuilder;
import org.mao.tinyserver.handler.RequestHandler;
import org.mao.tinyserver.requests.Request;
import org.mao.tinyserver.response.Response;
import org.mao.tinyserver.ssl.SSLChannelFactory;
import org.mao.tinyserver.utils.BytesUtil;
import org.mao.tinyserver.utils.IOUtils;
import org.mao.tinyserver.utils.LoggerUtil;
import sun.nio.ch.IOUtil;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {
    public static ServerConfig serverConfig;
    private final Selector selector;
    private static final Logger logger = LoggerUtil.getLogger(Server.class);

    private SSLContext sslContext;

    public static Map<String, Response> cache;


    public Server(String configURL) throws IOException {
        selector = Selector.open();
        serverConfig = new ServerConfigBuilder(configURL).build();
    }

    public void run() {
        if (!init()){
            return;
        }
        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "selector.select() error");
            }
            Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                try {
                    if (key.isAcceptable()) {
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel channel = server.accept();
                        //System.out.println("aaaaa" + channel.getRemoteAddress().toString());
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                    }else if (key.isWritable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        Response response = (Response) key.attachment();

                        if (response.send(true, client)) {
                            key.cancel();
                            client.close();
                        }

                    } else if (key.isReadable()) {
                        SocketChannel client = (SocketChannel) key.channel();
                        ThreadPool.execute(new RequestHandler(client, selector, key, this));
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "socket channel error");
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                        logger.log(Level.SEVERE, "closing socket channel error");
                    }
                } finally {
                    keys.remove();
                }
            }
        }
    }

    private boolean init(){
        long start = System.currentTimeMillis();
        ServerSocketChannel serverChannel = null;
        try {
            serverChannel = ServerSocketChannel.open();
            ServerSocket  serverSocket = serverChannel.socket();
            InetSocketAddress localPort = new InetSocketAddress(serverConfig.getPort());
            serverSocket.bind(localPort);
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e){
            logger.log(Level.SEVERE, "Initialize server error! Please check whether the port has already been in use");
            if (serverChannel != null) {
                try {
                    serverChannel.close();
                } catch (IOException e1) {
                    logger.log(Level.SEVERE, "close channel error");
                }
            }
            return false;
        }
        // Deal with HTTPS
        if (serverConfig.getSslKeyStore() != null && serverConfig.getSslPassWord() != null){
            File file = null;
            if (serverConfig.getSslKeyStore().startsWith("classpath:")) {
                byte[] fileBytes = IOUtils.getByteByInputStream(Server.class.getResourceAsStream(serverConfig.getSslKeyStore().substring("classpath:".length())));
                try {
                    file = File.createTempFile("keystore", serverConfig.getSslKeyStore().substring(serverConfig.getSslKeyStore().lastIndexOf(".")));
                    BytesUtil.writeBytesToFile(fileBytes, file);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "", e);
                }

            } else {
                file = new File(serverConfig.getSslKeyStore());
            }
            if (file == null || !file.exists()) {
                throw new RuntimeException("keystore can't null or not exists");
            } else {
                try {
                    sslContext = SSLChannelFactory.getSSLContext(file, serverConfig.getSslPassWord());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "", e);
                }
            }
        }
        long spentTime = System.currentTimeMillis() - start;
        logger.log(Level.INFO, "Server Initialization success! Server address is http://localhost:"
                    + serverConfig.getPort()
                    + "\n total time : " + spentTime + "ms");       //todo:  ip
        return true;
    }

    public void destroy() {
        if (selector == null){
            return;
        }
        try {
            selector.close();
            logger.log(Level.INFO, "close selector success");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "close selector error", e);
        }
    }

    public ReadWriteSelectorHandler getRWHandler(SocketChannel client, SelectionKey key){
        ReadWriteSelectorHandler rwHandler = null;
        if (serverConfig.getSslKeyStore() != null && serverConfig.getSslPassWord() != null){
            try {
                rwHandler = new SSLReadWriteSelectorHandler(client, key, sslContext, selector);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            rwHandler = new PlainReadWriteSelectorHandler(client);
        }
        return rwHandler;
    }


}
