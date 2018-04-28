import org.mao.tinyserver.io.Server;

import java.io.IOException;

public class ServerTest {

    public static void main(String[] args) throws IOException {

            // read the single file to serve
            Server server = new Server("/Users/garymao/NustoreFiles/Nutstore/github/TinyServer/src/test/resources/ServerConfig.xml");
            server.run();

    }
}
