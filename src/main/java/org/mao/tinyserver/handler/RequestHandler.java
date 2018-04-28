package org.mao.tinyserver.handler;

import org.mao.tinyserver.exception.IllegalRequestException;
import org.mao.tinyserver.exception.ServerInternalException;
import org.mao.tinyserver.io.Server;
import org.mao.tinyserver.requests.Request;
import org.mao.tinyserver.requests.RequestParser;
import org.mao.tinyserver.response.Response;
import org.mao.tinyserver.response.ResponseBuilder;
import org.mao.tinyserver.response.enums.Status;
import org.mao.tinyserver.utils.LoggerUtil;

import java.io.*;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable {

    private static final Logger LOGGER = LoggerUtil.getLogger(RequestHandler.class);
    private final SocketChannel channel;
    private final Selector selector;
    private ResponseBuilder responseBuilder;


    public RequestHandler(SocketChannel client, Selector selector) {
        this.channel = client;
        this.selector = selector;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        Request request = null;
        Response response;
        try {
            request = new RequestParser().parseRequest(channel);       // 有可能是null
            LOGGER.log(Level.INFO, "request is :\n" + request.toString());

            // todo: request可能还有问题, 待测试
            responseBuilder = new ResponseBuilder(request);
            response = tryStaticFile(request);

            //service
//            if (response == null){
//
//            }


        } catch (ServerInternalException e1) { // 这个IOException都是parseRequest里出来的
            LOGGER.log(Level.SEVERE, e1.getMessage());
            response = new ResponseBuilder(request).notOkResponse(Status.INTERNAL_SERVER_ERROR_500);
        } catch (IllegalRequestException e2) {
            response = new ResponseBuilder(request).notOkResponse(Status.BAD_REQUEST_400);
        } catch (Exception e3) {
            LOGGER.log(Level.SEVERE, "Unknown error : " + e3.getMessage());
            e3.printStackTrace();
            response = new ResponseBuilder(request).notOkResponse(Status.INTERNAL_SERVER_ERROR_500);
        }

        //没有一个service符合, 返回404
        if (response == null){
            response = new ResponseBuilder(request).notOkResponse(Status.NOT_FOUND_404);
        }

        attachResponse(response);

        if (request != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Request handled, ").append(request.getMethod()).append(" / ").append(request.getUrl())
                    .append(" / ").append(response.getStatus()).append(" / ")
                    .append(System.currentTimeMillis() - start);
            LOGGER.log(Level.INFO, sb.toString());
        }



//            ServiceMethodInfo service = ServiceRegistry.findService(request);
//            if (service == null) {
//                response = new NotFoundResponse();
//            } else if (!service.containHttpMethod(request.getMethod())) {
//                response = new Response(Status.METHOD_NOT_ALLOWED_405);
//            } else {
//                response = (Response) service.invoke(request);
//                if (response == null) {
//                    throw new ServerInternalException("service返回了一个null");
//                }
//            }
//


    }

    private Response tryStaticFile(Request request){
        if ((request.getUri()+"/").startsWith(Server.serverConfig.getStaticURLPath()+"/")) {
            String path = request.getUri().substring(Server.serverConfig.getStaticURLPath().length());
//            if (Server.serverConfig.getStaticURLPath().equals(request.getUri().endsWith("/") ? request.getUri() : (request.getUri() + "/"))) {
//                path += Server.serverConfig.getWelcomeFile();
//            }
            File file = new File(Server.serverConfig.getStaticLocationPath() + path);
            return responseBuilder.fileResponse(file, path);

        }
        return null;
    }

    private void attachResponse(Response response) {
        try {
            channel.register(selector, SelectionKey.OP_WRITE, response);
            selector.wakeup();
        } catch (ClosedChannelException e) {
            LOGGER.log(Level.SEVERE, "通道已关闭", e);
        }
    }


}
