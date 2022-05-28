package net.zhuruoling.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.zhuruoling.broadcast.UdpBroadcastReceiver;
import net.zhuruoling.configuration.ConfigReader;
import net.zhuruoling.configuration.Configuration;
import net.zhuruoling.kt.TryKotlin;
import net.zhuruoling.server.HttpServer;
import net.zhuruoling.server.SocketServer;
import net.zhuruoling.session.SessionInitialServer;
import net.zhuruoling.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    final static Logger logger = LoggerFactory.getLogger("Main");

    static Configuration config = null;
    static boolean isInit = false;

    public static void main(String [] args) throws IOException {
        TryKotlin.INSTANCE.printOS();
        boolean isExampleGen = false;
        if (args.length >= 1) {
            if (Objects.equals(args[0], "--exampleGenerate")){
                logger.info("Generating examples.");
                Util.generateExample();
                System.exit(0);
            }
        }

        logger.info("Hello World!");

        if (!Util.fileExists(Util.getWorkingDir() + File.separator + "config.json")) {
            isInit = true;
            Util.createConfig(logger);
        }
        for (String folder : Util.dataFolders.clone()) {
            File file = new File(Util.getWorkingDir() + File.separator + folder);
            if (!file.exists() || !file.isDirectory())
                isInit = true;
        }


        if (isInit){
            logger.info("Preparing for data folders.");
            for (String folder : Util.dataFolders.clone()) {
                Util.createFolder(Util.getWorkingDir() + File.separator + folder, logger);
            }
            System.exit(0);
        }


        config = ConfigReader.read();
        if (config == null){
            logger.error("Empty CONFIG.");
            System.exit(1);
        }

        Util.listAll(logger);



        logger.info("Server key:" + config.getKey() + " Server crypto key:" + config.getCryptoKey());
        logger.info("Launching...");

        var socketServer = new SessionInitialServer();
        socketServer.start();
        var receiver = new UdpBroadcastReceiver();
        receiver.start();
        var httpServerKt = new HttpServer();
        httpServerKt.start();


        while (true){
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            if (Objects.equals(line, "stop")){
                break;
            }
        }
        receiver.interrupt();
        socketServer.interrupt();
        httpServerKt.interrupt();
        System.exit(0);
        //logger.info("Exit.");
    }
}
