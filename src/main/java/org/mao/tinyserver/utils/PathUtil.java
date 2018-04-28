package org.mao.tinyserver.utils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class PathUtil {

    public static String getRootPath(){
        String path;
        if (PathUtil.class.getResource("/") != null) {
            String tPath = PathUtil.class.getResource("/").getPath();
            try {
                path = URLDecoder.decode(tPath, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                //e.printStackTrace();
                path = tPath;
            }
            path = new File(path).getParentFile().getParentFile().toString();
        } else {
            if (PathUtil.class.getProtectionDomain() != null) {
                String tPath = PathUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath().replace("\\", "/");
                try {
                    path = URLDecoder.decode(tPath, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    //e.printStackTrace();
                    path = tPath;
                }
                if ("/".equals(File.separator)) {
                    path = path.substring(0, path.lastIndexOf('/'));
                } else {
                    path = path.substring(1, path.lastIndexOf('/'));
                }
            } else {
                path = "/";
            }
        }
        return path;
    }

    public static String getTempPath() {
        String str = getRootPath() + "/temp/";
        new File(str).mkdirs();
        return str;
    }
}
