package org.mao.tinyserver.requests;

import org.junit.Test;
import org.mao.tinyserver.config.ServerConfig;
import org.mao.tinyserver.config.ServerConfigBuilder;
import org.mao.tinyserver.exception.IllegalRequestException;
import org.mao.tinyserver.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class RequestParserTest {

    private static final String URI = "/";
    public static final String CONTENT_LENGTH = "Content-Length: ";
    public static final String FIELD = "part1";
    private static final String FIELD2 = "part2";
    private static final String VALUE2 = "value2";

    // todo: 没有serverConfig, 没法儿测
    @Test
    public void testPostWithMultipartFormFieldsAndFile() throws IOException, IllegalRequestException {           //todo: 还要再写几个测试
        String fileName = "FieldsAndFile.txt";
        String fileContent = "Content\r\nContent\r\nContent";

        String divider = UUID.randomUUID().toString();
        String header = "POST " + URI + " HTTP/1.1\r\nContent-Type: " + "multipart/form-data; boundary=" + divider + "\r\n";
        String content =
                "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\"" + FIELD + "\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: image/jpeg\r\n" + "\r\n" + fileContent + "\r\n" + "--" + divider + "\r\n" + "Content-Disposition: form-data; name=\""
                        + FIELD2 + "\"\r\n" + "\r\n" + VALUE2 + "\r\n" + "--" + divider + "--\r\n";
        int size = content.length();
        String input = header + CONTENT_LENGTH + size + "\r\n\r\n" + content;

        System.out.println(input);

        Request request = new RequestParser().parseRequest(input.getBytes());

        System.out.println(request);
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put(FIELD, new ArrayList<>(Arrays.asList(fileName)));
        paramMap.put(FIELD2, new ArrayList<>(Arrays.asList(VALUE2)));
        assertEquals(paramMap, request.paramMap);
        Map<String, File> files = new HashMap<>();
        files.put(FIELD, new File(PathUtil.getTempPath() + fileName));
        assertEquals(files, request.files);
    }


}