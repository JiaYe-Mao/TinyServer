package org.mao.tinyserver.requests;


import org.mao.tinyserver.config.ServerConfig;
import org.mao.tinyserver.exception.ServerInternalException;
import org.mao.tinyserver.exception.IllegalRequestException;
import org.mao.tinyserver.io.ReadWriteSelectorHandler;
import org.mao.tinyserver.io.Server;
import org.mao.tinyserver.requests.enums.HttpMethod;
import org.mao.tinyserver.requests.enums.HttpScheme;
import org.mao.tinyserver.utils.BytesUtil;
import org.mao.tinyserver.utils.LoggerUtil;
import org.mao.tinyserver.utils.PathUtil;


import java.io.*;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;


public class RequestParser {

    private static final Logger LOGGER = LoggerUtil.getLogger(RequestParser.class);
    private static final String CRLF = "\r\n";
    static final String SPLIT = CRLF + CRLF;
    private static final int MULTIPART_HEADER_STATE = 0;
    private static final int MULTIPART_CONTENT_STATE = 1;
    public static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";
    public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, Pattern.CASE_INSENSITIVE);

    public static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";
    public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

    public static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";
    public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

    private final Request request = new Request();

    public Request parseRequest(SocketChannel channel, SelectionKey key, Server server) throws IllegalRequestException {
        // TODO: 不把request都取出来, 只把head取出来, 然后把后面的直接放到ByteBuffer里去
        request.server = server;
        request.client = channel;

        ReadWriteSelectorHandler rwHandler = request.getRwHandler(key);

        //byte[] requestBytes = BytesUtil.getBytesFromChannel(channel, Server.serverConfig.getMaxRequestKb());  //IllegalRequestException
        byte[] requestBytes = new byte[0];
        try {
            requestBytes = rwHandler.handleRead().array();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return parseRequest(requestBytes);
    }

    public Request parseRequest(byte[] requestBytes) throws IllegalRequestException {
        request.characterEncoding = Server.serverConfig.getCharacterEncoding();

        String fullDataStr = null;              //todo cookie和HTTPSession
        try {
            fullDataStr = new String(requestBytes, request.characterEncoding);
        } catch (UnsupportedEncodingException e) {
            //ignored
        }
        int position = fullDataStr.indexOf(SPLIT);
        if (position == -1) {
            throw new IllegalRequestException("illegal request format! Request do not have \\r\\n\\r\\n");
        }

        parseHttpMethod(fullDataStr);
        request.headerStr = fullDataStr.substring(0, position);
        String headerArr[] = request.headerStr.split(CRLF);
        // parse HttpHeader
        try {
            parseHttpProtocolHeader(headerArr);
        } catch (Exception e) {
            throw new IllegalRequestException("parsing header error");
        }
        int headerByteLength = request.headerStr.getBytes().length + SPLIT.getBytes().length;
        byte[] requestBody = BytesUtil.subBytes(requestBytes, headerByteLength, requestBytes.length - headerByteLength);
        parseHttpRequestBody(requestBody);


        return request;
    }

    private void parseHttpMethod(String fullDataStr) throws IllegalRequestException {
        if (request.getMethod() == null) {
            boolean check = false;
            for (HttpMethod httpMethod : HttpMethod.values()) {
                if (fullDataStr.startsWith(httpMethod.name() + " ")) {
                    request.method = httpMethod;
                    check = true;
                    break;
                }
            }
            if (!check) {
                throw new IllegalRequestException("Illegal http method!");
            }
        }
    }

    private void parseHttpProtocolHeader(String[] headerArr) throws UnsupportedEncodingException {
        // First line
        String pHeader = headerArr[0];
        String[] protocolHeaderArr = pHeader.split(" ");
        String tUrl = request.uri = protocolHeaderArr[1];
        request.scheme = HttpScheme.parseScheme(protocolHeaderArr[2]);
        // just for some proxy-client
//        if (tUrl.startsWith(request.getScheme() + "://")) {
//            tUrl = tUrl.substring((request.getScheme() + "://").length());
//            if (tUrl.contains("/")) {
//                request.header.put("Host", tUrl.substring(0, tUrl.indexOf("/")));
//                tUrl = tUrl.substring(tUrl.indexOf("/"));
//            } else {
//                request.header.put("Host", tUrl);
//                tUrl = "/";
//            }
//        }
        if (tUrl.contains("?")) {                        // Get方法, 否则querryString为null
            request.uri = tUrl.substring(0, tUrl.indexOf("?"));
            request.queryString = tUrl.substring(tUrl.indexOf("?") + 1);
            wrapperParamStrToMap(request.queryString);
        } else {
            request.uri = tUrl;
        }
        if (request.uri.contains("/")) {
            request.uri = URLDecoder.decode(request.uri.substring(request.uri.indexOf("/")), request.characterEncoding);
        } else {
            request.getHeaderMap().put("Host", request.uri);
            request.uri = "/";
        }
        // 先得到请求头信息
        for (int i = 1; i < headerArr.length; i++) {
            dealRequestHeaderString(headerArr[i]);
        }
    }

    private void dealRequestHeaderString(String str) {
        if (str.contains(":")) {
            request.header.put(str.split(":")[0], str.substring(str.indexOf(":") + 1).trim());
        }
    }

    private void parseHttpRequestBody(byte[] requestBodyData) throws IllegalRequestException {
        boolean flag;
        if (!isNeedEmptyRequestBody()){
            Object contentLengthObj = request.getHeader("Content-Length");
            if (contentLengthObj != null) {
                Integer dataLength = Integer.parseInt(contentLengthObj.toString());
//                if (dataLength > 20971520) {      //todo: getRequest().getRequestConfig().getMaxRequestBodySize()
//                    throw new IllegalRequestException("The Content-Length outside the max upload size " + 20971520);
//                }
                request.requestBodyBuffer = ByteBuffer.allocate(dataLength);
                request.requestBodyBuffer.put(requestBodyData);
                flag = !request.requestBodyBuffer.hasRemaining();                // 判断Content-Length是否符合body长度, 其实可以不要的
                if (flag) {
                    dealRequestBodyData();
                } else {
                    throw new IllegalRequestException("Content-Length not real");
                }
            } else {
                throw new ServerInternalException("lack Content-Length");
            }
        }
    }

    private void wrapperParamStrToMap(String paramStr) {
        request.paramMap = new HashMap<>();
        if (paramStr != null) {
            Map<String, List<String>> tempParam = new HashMap<>();
            String args[] = paramStr.split("&");
            for (String string : args) {
                int idx = string.indexOf("=");
                if (idx != -1) {
                    String key = string.substring(0, idx);
                    String value = string.substring(idx + 1);
                    if (tempParam.containsKey(key)) {
                        tempParam.get(key).add(value);
                    } else {
                        List<String> paramValues = new ArrayList<>();
                        paramValues.add(value);
                        tempParam.put(key, paramValues);
                    }
                }
            }
            request.paramMap = tempParam;
        }
    }

    private void dealRequestBodyData() throws IllegalRequestException {          //todo 待测试
        String contentType = request.header.get("Content-Type");
        if (contentType != null && contentType.split(";")[0] != null) {
            if ("application/x-www-form-urlencoded".equals(contentType.split(";")[0])){
                wrapperParamStrToMap(new String(request.requestBodyBuffer.array()));
            } else if ("multipart/form-data".equals(contentType.split(";")[0])) {
                int boundaryValueIndex = contentType.indexOf("boundary=");
                String boundary = contentType.substring(boundaryValueIndex + 9);   // 9是 `boundary=` 长度

                decodeMultipartFormData(request.requestBodyBuffer, boundary);

            } else {
                // raw or binary
                // do nothing. User can get body inputStream from request.
            }
        }
    }

    private void decodeMultipartFormData(ByteBuffer bodyBuffer, String boundary) throws IllegalRequestException {
        BufferedReader bin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bodyBuffer.array())));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            line = bin.readLine();
            sb.append(line).append(CRLF);      //todo: just for test
            if (line == null || !line.contains(boundary)) {
                throw new IllegalRequestException("BAD REQUEST: Content type is multipart/form-data but chunk does not start with boundary.");
            }

            String partName = null, fileName = null, partContentType = null;

            int state = MULTIPART_HEADER_STATE;               // 状态机
            int pcount = 0;

            StringBuilder partStr = null;

            while ((line = bin.readLine()) != null) {
                sb.append(line).append(CRLF);             //todo: just for test

                if (line.trim().length() == 0){              // current line is blank, which means the next line is the start of content.
                    state = MULTIPART_CONTENT_STATE;
                    continue;
                }

                if (line.contains(boundary)) {
                    if (request.paramMap == null){
                        request.paramMap = new HashMap<>();
                    }
                    List<String> values = request.paramMap.get(partName);
                    if (values == null) {
                        values = new ArrayList<String>();
                        request.paramMap.put(partName, values);
                    }
                    partStr.delete(partStr.lastIndexOf("\n"), partStr.lastIndexOf("\n")+1);       // delete last "\n"
                    if (partStr != null){
                        if (partContentType == null){
                            values.add(partStr.toString());
                        } else {
                            File file = new File(PathUtil.getTempPath() + fileName);
                            BytesUtil.writeBytesToFile(partStr.toString().getBytes(), file);

                            if (request.files == null) {
                                request.files = new HashMap<>();
                            }

                            if (!request.files.containsKey(partName)) {
                                request.files.put(partName, file);
                            } else {
                                int count = 2;
                                while (request.files.containsKey(partName + count)) {
                                    count++;
                                }
                                request.files.put(partName + count, file);
                            }
                            values.add(fileName);
                        }
                    }

                    partName = null; fileName = null; partContentType = null;
                    partStr = null;
                    state = MULTIPART_HEADER_STATE;
                    continue;     // next part
                }

                if (state == MULTIPART_HEADER_STATE) {
                    Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        String attributeString = matcher.group(2);
                        matcher = CONTENT_DISPOSITION_ATTRIBUTE_PATTERN.matcher(attributeString);
                        while (matcher.find()) {
                            String key = matcher.group(1);
                            if ("name".equalsIgnoreCase(key)) {
                                partName = matcher.group(2);
                            } else if ("filename".equalsIgnoreCase(key)) {
                                fileName = matcher.group(2);
                                if (!fileName.isEmpty()) {
                                    if (pcount > 0)
                                        partName = partName + String.valueOf(pcount++);
                                    else
                                        pcount++;
                                }
                            }
                        }
                    }
                    matcher = CONTENT_TYPE_PATTERN.matcher(line);
                    if (matcher.matches()) {
                        partContentType = matcher.group(2).trim();
                    }
                }

                if (state == MULTIPART_CONTENT_STATE){
                    // Read the part into a string
                    if (partStr == null){
                        partStr = new StringBuilder();
                    }
                    partStr.append(line).append("\n");
                }

            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        } finally {
            try {
                bin.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }

        System.out.println(sb.toString());      // todo: just for test
    }


    private boolean isNeedEmptyRequestBody() {
        return request.method == HttpMethod.GET || request.method == HttpMethod.CONNECT || request.method == HttpMethod.TRACE;
    }

}
