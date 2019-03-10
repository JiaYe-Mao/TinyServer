package org.mao.tinyserver.requests;

import org.mao.tinyserver.interfaces.HttpRequest;
import org.mao.tinyserver.io.PlainReadWriteSelectorHandler;
import org.mao.tinyserver.io.ReadWriteSelectorHandler;
import org.mao.tinyserver.io.SSLReadWriteSelectorHandler;
import org.mao.tinyserver.io.Server;
import org.mao.tinyserver.requests.enums.HttpMethod;
import org.mao.tinyserver.requests.enums.HttpScheme;
import org.mao.tinyserver.utils.LoggerUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Request implements HttpRequest {

    private static final Logger logger = LoggerUtil.getLogger(Request.class);
    protected String uri;                                // protected的目的: 只有包下的程序可以set, 用户只能get
    protected Map<String, List<String>> paramMap;         //paramMap: 如果是Get方法里面是queryString里解析, 如果是Post方法则是body的解析
    protected String queryString;
    protected SocketChannel client;

    protected Map<String, File> files;          // 所有multipart都在paramMap里, 如果multipart有Content-Type则将文件路径放入files
                                                // key为multipart的name, value是file

    protected HttpMethod method;
    protected Map<String, String> header = new HashMap<>();
    protected String headerStr;
    private Cookie[] cookies;
    protected HttpScheme scheme;
    protected ByteBuffer requestBodyBuffer;              // 放在buffer里(堆外), 不怕OOM
    private InputStream inputStream;
    protected String characterEncoding;
    protected Server server;

    protected SelectionKey key;

    private ReadWriteSelectorHandler rwHandler;

    public ReadWriteSelectorHandler getRwHandler(SelectionKey key) {
        if (rwHandler == null){
            this.rwHandler = server.getRWHandler(client, key);
        }
        return rwHandler;
    }

    public SelectionKey getKey(){
        return key;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public void setFiles(Map<String, File> files) {
        this.files = files;
    }

    @Override
    public Map<String, List<String>> getParamMap() {
        return paramMap;
    }

    @Override
    public String getHeader(String key) {
        String headerValue = header.get(key);
        if (headerValue != null) {
            return headerValue;
        }
        for (Map.Entry<String, String> entry : header.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public String getRemoteHost() {
        return ((InetSocketAddress) client.socket().getRemoteSocketAddress()).getHostString();
    }

    public HttpMethod getMethod() {
        return method;
    }

    @Override
    public String getUrl() {
        return getScheme() + "://" + header.get("Host") + uri;
    }

    public String getrootUrl(){
        return getScheme() + "://" + header.get("Host");
    }


//    @Override
//    public Cookie[] getCookies() {
//        if (cookies == null) {
//            dealWithCookie(false);
//        }
//        if (cookies == null) {
//            //avoid not happen NullPointException
//            cookies = new Cookie[0];
//        }
//        return cookies;
//    }
//
//    private void dealWithCookie(boolean create) {
//        if (!requestConfig.isDisableCookie()) {
//            String cookieHeader = header.get("Cookie");
//            if (cookieHeader != null) {
//                cookies = Cookie.saxToCookie(cookieHeader);
//                String jsessionid = Cookie.getJSessionId(cookieHeader);
//                if (jsessionid != null) {
//                    `session = SessionUtil.getSessionById(jsessionid);
//                }
//            }
//            if (create && session == null) {
//                if (cookies == null) {
//                    cookies = new Cookie[1];
//                } else {
//                    cookies = new Cookie[cookies.length + 1];
//                }
//                Cookie cookie = new Cookie(true);
//                String jsessionid = UUID.randomUUID().toString();
//                cookie.setName(Cookie.JSESSIONID);
//                cookie.setPath("/");
//                cookie.setValue(jsessionid);
//                cookies[cookies.length - 1] = cookie;
//                session = new HttpSession(jsessionid);
//                SessionUtil.sessionMap.put(jsessionid, session);
//                LOGGER.info("create a cookie " + cookie.toString());
//            }
//        }
//    }

    @Override
    public String getParaToStr(String key) {
        if (paramMap.get(key) != null) {
            try {
                return URLDecoder.decode(paramMap.get(key).get(0), characterEncoding);
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.SEVERE, "", e);
            }
        }
        return null;
    }


    @Override
    public int getParaToInt(String key) {
        if (paramMap.get(key) != null) {
            return Integer.parseInt(paramMap.get(key).get(0));
        }
        return 0;
    }

    @Override
    public boolean getParaToBool(String key) {
        return paramMap.get(key) != null && "on".equals(paramMap.get(key).get(0));
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public String getFullUrl() {
        if (queryString != null) {
            return getUrl() + "?" + queryString;
        }
        return getUrl();
    }

    @Override
    public String getQueryStr() {
        return queryString;
    }


    @Override
    public HttpScheme getScheme() {
        return scheme;
    }

    @Override
    public Map<String, String> getHeaderMap() {
        return header;
    }

    @Override
    public InputStream getInputStream() {
        if (inputStream != null) {
            return inputStream;
        } else {
            if (requestBodyBuffer != null) {
                inputStream = new ByteArrayInputStream(requestBodyBuffer.array());    //array()获得byte[]
            } else {
                inputStream = new ByteArrayInputStream(new byte[]{});
            }
            return inputStream;
        }
    }

    @Override
    public Map<String, List<String>> decodeParamMap() {
        Map<String, List<String>> encodeMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : getParamMap().entrySet()) {
            List<String> strings = new ArrayList<>();
            for (int i = 0; i < entry.getValue().size(); i++) {
                try {
                    strings.add(URLDecoder.decode(entry.getValue().get(i), characterEncoding));
                } catch (UnsupportedEncodingException e) {
                    logger.log(Level.SEVERE, "decode error", e);
                }
            }
            encodeMap.put(entry.getKey(), strings);
        }
        return encodeMap;
    }


    public ByteBuffer getInputByteBuffer() {
        byte[] splitBytes = RequestParser.SPLIT.getBytes();
        byte[] bytes = headerStr.getBytes();
        if (requestBodyBuffer == null) {
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + splitBytes.length);
            buffer.put(bytes);
            buffer.put(splitBytes);
            return buffer;
        } else {
            byte[] dataBytes = requestBodyBuffer.array();
            ByteBuffer buffer = ByteBuffer.allocate(headerStr.getBytes().length + splitBytes.length + dataBytes.length);
            buffer.put(headerStr.getBytes());
            buffer.put(splitBytes);
            buffer.put(dataBytes);
            return buffer;
        }
    }


    @Override
    public ByteBuffer getRequestBodyByteBuffer() {
        return requestBodyBuffer;
    }

    @Override
    public String toString() {
        return headerStr;
    }


}
