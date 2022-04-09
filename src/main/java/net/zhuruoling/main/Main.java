package net.zhuruoling.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.zhuruoling.broadcast.UdpBroadcastReceiver;
import net.zhuruoling.configuration.ConfigReader;
import net.zhuruoling.configuration.Configuration;
import net.zhuruoling.kt.TryKotlinKt;
import net.zhuruoling.scontrol.SControlClient;
import net.zhuruoling.scontrol.SControlClientFileReader;
import net.zhuruoling.server.HTTPServer;
import net.zhuruoling.server.SocketServer;
import net.zhuruoling.util.Util;
import net.zhuruoling.whitelist.Whitelist;
import net.zhuruoling.whitelist.WhitelistReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class Main {
    final static Logger logger = LoggerFactory.getLogger("Main");

    static Configuration config = null;
    static boolean isInit = false;

    public static void main(String [] args) {
        TryKotlinKt.printOS();
        boolean isExampleGen = false;
        if (args.length >= 1) {
            if (Objects.equals(args[0], "--exampleGenerate")){
                logger.info("Generating examples.");
                isExampleGen = true;
                isInit = true;
            }
            else if (Objects.equals(args[0], "--testCrypto")){
                Util.testCrypto();
                System.exit(0);
            }
        }

        logger.info("Hello World!");

        if (!new Util().fileExists(Util.getWorkingDir() + File.separator + "config.json")){
            logger.warn("CONFIG NOT EXIST,creating.");
            try {
                if (!new Util().createFile(Util.getWorkingDir() + File.separator + "config.json")){
                    logger.error("Unable to create file:" + Util.getWorkingDir() + File.separator + "config.json");
                    System.exit(-1);
                }
                logger.info("Created Config,writing default config.");
                Gson gson = new GsonBuilder().serializeNulls().create();
                String cont = gson.toJson(new Configuration(50000,"Uranium"));
                File fp = new File(Util.getWorkingDir() + File.separator + "config.json");
                FileOutputStream stream = new FileOutputStream(fp);
                OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                writer.append(cont);
                writer.close();
                stream.close();
                logger.info("Created Config.");
                isInit = true;
            } catch (IOException e) {
                e.printStackTrace();
                isInit = true;
            }
        }
        File folder = new File(Util.getWorkingDir() + File.separator + "clients");

        if (!folder.exists() && !folder.isDirectory()) {
            logger.warn("No Uranium sControl Client added.");
            folder.mkdirs();
            logger.warn("created Uranium sControl Client folder.");
            isInit = true;
        }
        if (isExampleGen) {
           Util.generateExample();
        }
        folder = new File(Util.getWorkingDir() + File.separator + "whitelists");
        if (!folder.exists() && !folder.isDirectory()) {
            logger.warn("No Uranium whitelist added.");
            folder.mkdirs();
            logger.warn("created Uranium whitelist folder.");
            isInit = true;
        }

        if (isInit){
            System.exit(0);
        }


        config = ConfigReader.read();
        if (config == null){
            logger.error("Empty CONFIG.");
        }

        logger.info("Listing qControl Clients:");
        SControlClientFileReader reader = new SControlClientFileReader();
        if (reader.isFail()){
            logger.error("Failed to read qControl Clients.");
            System.exit(1);
        }
        if (!reader.isNoClient()) {
            List<SControlClient> clientList = reader.getClientList();
            clientList.forEach(client -> logger.info( "  -" + client.toString()));
        }
        else {
            logger.warn("No qControl Clients added.");
        }

        logger.info("Listing Whitelist contents:");
        WhitelistReader reader_ = new WhitelistReader();
        if (reader.isFail()){
            logger.error("Failed to read Whitelists.");
            System.exit(1);
        }
        if (!reader_.isNoWhitelist()) {
            List<Whitelist> whitelists = reader_.getWhitelists();
            whitelists.forEach(client -> logger.info( "  -" + client.toString()));
        }
        else {
            logger.warn("No Whitelist added.");
        }

        logger.info("Server key:" + config.getKey() + " Server crypto key:" + config.getCryptoKey());
        logger.info("Launching...");
        var socketServer = new SocketServer();
        socketServer.start();
        var httpServer = new HTTPServer();
        httpServer.start();
        var receiver = new UdpBroadcastReceiver();
        receiver.start();
        while (true){

        }
        //logger.info("Exit.");
    }
}
