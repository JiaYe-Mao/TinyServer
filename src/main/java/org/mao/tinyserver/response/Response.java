package org.mao.tinyserver.response;

import org.mao.tinyserver.exception.ServerInternalException;
import org.mao.tinyserver.io.Server;
import org.mao.tinyserver.requests.Cookie;
import org.mao.tinyserver.requests.Request;
import org.mao.tinyserver.response.enums.Status;
import org.mao.tinyserver.utils.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Response {

    private static final Logger LOGGER = LoggerUtil.getLogger(Response.class);
    private static final String CRLF = "\r\n";
    private Status status;
    protected Map<String, String> headers = new HashMap<>();
    protected ByteBuffer data;
    protected List<Cookie> cookieList = new ArrayList<>();
    protected Request request;

    public Response(Request request){
        this.request = request;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void addHeader(String name, String value){
        headers.put(name, value);
    }

    public boolean send(boolean close, SocketChannel sc) {
        try {
            byte[] head = wrapperBaseResponseHeader();
            ByteBuffer responseBB;
            if (data != null && data.position()>0) {
                data.flip();
                responseBB = ByteBuffer.allocate(head.length + data.limit());
                responseBB.put(head).put(data);

                //test
                responseBB.flip();
                byte[] bytes = new byte[head.length + data.limit()];
                responseBB.get(bytes);
                String resp = new String(bytes);
                System.out.println(resp);

            } else {
                responseBB = ByteBuffer.allocate(head.length);
                responseBB.put(head);
            }
            responseBB.flip();
            // Write()方法无法保证能写多少字节到SocketChannel。所以，我们重复调用write()直到Buffer没有要写的字节为止。
            while (responseBB.hasRemaining() && sc.isOpen()) {

                int len = sc.write(responseBB);
                if (len < 0) {
                    throw new EOFException();
                }
            }
            if ("close".equalsIgnoreCase(headers.get("Connection"))) {     // todo: close 条件
                try {
                    sc.close();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "close SocketChannel", e);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "send error " + e.getMessage());
            return false;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "send error", e);
            throw new ServerInternalException("send error", e);
        }
    }

    private byte[] wrapperBaseResponseHeader() {
//        if (responseConfig.isGzip()) {
//            header.put("Content-Encoding", "gzip");
//            header.remove("Content-Length");
//        }

        headers.put("Server", Server.serverConfig.getSERVER_INFO());
        if (!headers.containsKey("Connection")) {
            boolean keepAlive = false;
            if (request != null){
                keepAlive = request.getHeader("Connection") != null && "keep-alive".equalsIgnoreCase(request.getHeader("Connection"));
            }
            if (keepAlive) {
                headers.put("Connection", "keep-alive");       // todo; 其实就算request里没有connection也是keep-alive
            } else {
                headers.put("Connection", "close");
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(status.getRequestStatus()).append(" ").append(status.getMessage()).append(CRLF);
        for (Map.Entry<String, String> he : headers.entrySet()) {
            sb.append(he.getKey()).append(": ").append(he.getValue()).append(CRLF);
        }
        //deal cookie
//        if (!responseConfig.isDisableCookie()) {
//            Cookie[] cookies = request.getCookies();
//            if (cookies != null) {
//                for (Cookie cookie : cookies) {
//                    if (cookie != null && cookie.isCreate()) {
//                        cookieList.add(cookie);
//                    }
//                }
//            }
//            for (Cookie cookie : cookieList) {
//                sb.append("Set-Cookie: ").append(cookie).append(CRLF);
//            }
//        }
        sb.append(CRLF);
        return sb.toString().getBytes();
    }



}
