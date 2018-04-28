package org.mao.tinyserver.utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MimeTypeUtil {

    private static final Logger LOGGER = LoggerUtil.getLogger(MimeTypeUtil.class);
    private static Map<String, String> mimeTypes = new HashMap<String, String>();


    static {
        Properties prop = new Properties();
        try {
            prop.load(MimeTypeUtil.class.getResourceAsStream("/mimetype.properties"));
            for (Entry<Object, Object> p : prop.entrySet()) {
                mimeTypes.put(p.getKey().toString(), p.getValue().toString());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }
    }



    public static String findContentType(String path) {
        int idx = path.lastIndexOf('.');
        if (idx == -1 || idx == path.length() - 1) {
            return "text/plain";
        }
        String suffix = path.substring(idx + 1, path.length());
        return mimeTypes.getOrDefault(suffix, "text/plain");
    }
}
