package net.zhuruoling.session;

import net.zhuruoling.configuration.ConfigReader;
import net.zhuruoling.server.SocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Objects;

public class SessionInitialServer extends Thread {

    public SessionInitialServer(){
        this.setName("SessionInitialServer#" + this.getId());
    }

    Logger logger = LoggerFactory.getLogger("SessionInitialServer");
    @Override
    public void run() {
        try {
            logger.info("Started SessionInitialServer.");
            ServerSocket server = new ServerSocket(Objects.requireNonNull(ConfigReader.read()).getPort());
            var socket = server.accept();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
