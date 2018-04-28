package org.mao.tinyserver;

import org.mao.tinyserver.io.Server;

import java.io.IOException;

public class ServerRunner {

    public static void main(String[] args) {
        try {
            Server server = new Server("/Users/garymao/NustoreFiles/Nutstore/github/TinyServer/src/main/resources/ServerConfig.xml");
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
