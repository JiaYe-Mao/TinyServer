package org.mao.tinyserver.interfaces;


import org.mao.tinyserver.requests.Cookie;
import org.mao.tinyserver.requests.enums.HttpMethod;
import org.mao.tinyserver.requests.enums.HttpScheme;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface HttpRequest {

    Map<String, List<String>> getParamMap();

    Map<String, List<String>> decodeParamMap();

    String getHeader(String key);

    String getRemoteHost();

    String getUri();

    String getUrl();

    String getFullUrl();

//    String getRealPath();

    String getQueryStr();

    HttpMethod getMethod();

//    Cookie[] getCookies();

//    HttpSession getSession();

    boolean getParaToBool(String key);

    String getParaToStr(String key);

    int getParaToInt(String key);

//    File getFile(String key);

//    Map<String, Object> getAttr();

    HttpScheme getScheme();

    Map<String, String> getHeaderMap();

    InputStream getInputStream();

//    RequestConfig getRequestConfig();
//
//    ReadWriteSelectorHandler getHandler();

//    long getCreateTime();

    ByteBuffer getInputByteBuffer();

    ByteBuffer getRequestBodyByteBuffer();
}
