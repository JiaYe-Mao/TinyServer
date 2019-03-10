package org.mao.tinyserver.response;

import org.mao.tinyserver.io.Server;
import org.mao.tinyserver.requests.Request;
import org.mao.tinyserver.response.enums.Status;
import org.mao.tinyserver.utils.IOUtils;
import org.mao.tinyserver.utils.LoggerUtil;
import org.mao.tinyserver.utils.MimeTypeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResponseBuilder {

    private static final Logger LOGGER = LoggerUtil.getLogger(ResponseBuilder.class);
    private Response response;

    public ResponseBuilder(Request request){
        this.response = new Response(request);
    }

    public void build(){

    }

    public Response fileResponse(File file, String path) {
        if (!file.exists()){
            response = notOkResponse(Status.NOT_FOUND_404);
            return response;
        }
        if (file.isDirectory()){
            if (!Server.serverConfig.isFileManager()) {
                response = notOkResponse(Status.BAD_REQUEST_400);
                return response;
            } else {
                // 文件夹访问
                response.setStatus(Status.SUCCESS_200);
                response.headers.put("Content-Type", "text/html");

                byte[] body = getFileManagerHtml(file, path).getBytes();
                setBody(body);

                return response;
            }
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            response.setStatus(Status.SUCCESS_200);

            String mimeType = MimeTypeUtil.findContentType(file.getPath());
            response.headers.put("Content-Type", MimeTypeUtil.findContentType(file.getName()));

            response.data = IOUtils.convertInputStreamIntoByteBuffer(fileInputStream);   //todo test

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }

        return response;
    }

    public void setBody(byte[] body){
        response.headers.put("Content-Length", body.length + "");
        response.data = ByteBuffer.allocate(body.length);
        response.data.put(body);
    }

    public Response notOkResponse(Status status){
        response.setStatus(status);
        if (status.getRequestStatus() >= 400) {
            response.headers.put("Content-Type", "text/html");
            byte[] body = getHtmlStrByStatusCode(status).getBytes();
            setBody(body);
        } else if (status.getRequestStatus() >= 300) {
            if (!response.headers.containsKey("Location")) {
                String welcomeFile = Server.serverConfig.getWelcomeFile();
                if (welcomeFile == null || "".equals(welcomeFile.trim())) {
                    response.headers.put("Location", response.request.getScheme() + "://" +response.request.getHeader("Host") + "/" + response.request.getUri() + welcomeFile);
                }
            }
        }
        return response;
    }


    // html generator
    public String getHtmlStrByStatusCode(Status status) {
        return "<html><head><title>" + status.getRequestStatus() + " " + status.getMessage() + "</title></head><body><center><h1>" + status.getRequestStatus() + " " + status.getMessage() + "</h1></center><hr><center>" + Server.serverConfig.getSERVER_INFO() + "</center></body></html>";
    }

    public String getFileManagerHtml(File file, String path){
        if (path.endsWith("/")){
            path = path.substring(0, path.length()-1);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"en\" dir=\"ltr\"><head><meta charset=\"utf-8\"><title>")
                .append("/").append(path).append("</title></head><body><h1>Index of ")
                .append("/").append(path).append("</h1><form action=\"").append(response.request.getUrl()).append("\" method=\"post\" enctype=\"multipart/form-data\"><input type=\"file\" name=\"fileUpload\" />\n" +
                "    <input type=\"submit\" value=\"上传文件\" />\n" +
                "    </form><br><hr><br>");
        // parent dir
        if (!"".equals(path)){
            sb.append("<a href=\"").append(response.request.getUrl().substring(0, response.request.getUrl().lastIndexOf("/")))
                    .append("\">../</a><br>");
        }
        // todo: 上传下载
        String[] childFiles = file.list();
        for (String childFile : childFiles) {
            sb.append("<a href=\"").append(response.request.getrootUrl()).append(Server.serverConfig.getStaticURLPath())
                    .append("/").append(path);
            if (!"".equals(path)){
                sb.append("/");
            }
            sb.append(childFile).append("\">").append(childFile).append("</a><br>");
        }
        sb.append("<hr></body></html>");
        return sb.toString();
    }

}
