package net.ruogustudio.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ruogustudio.configuration.ConfigReader;
import net.ruogustudio.configuration.Configuration;
import net.ruogustudio.scontrol.SControlClient;
import net.ruogustudio.scontrol.SControlClientFileReader;
import net.ruogustudio.socket.ClientSocketHandler;
import net.ruogustudio.util.Util;
import net.ruogustudio.whitelist.Whitelist;
import net.ruogustudio.whitelist.WhitelistReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Main {
    final static Logger logger = LoggerFactory.getLogger("Main");

    static Configuration config = null;
    static boolean isInit = false;

    private static byte[] encryptECB(byte[] data, byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        var result = cipher.doFinal(data);
        return Base64.getEncoder().encode(result);
    }

    private static byte[] decryptECB(byte[] data, byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        byte[] base64 = Base64.getDecoder().decode(data);
        return cipher.doFinal(base64);
    }

    public static void main(String[] args) {
        boolean isExampleGen = false;
        if (args.length >= 1) {
            if (Objects.equals(args[0], "--exampleGenerate")){
                logger.info("Generating examples.");
                isExampleGen = true;
                isInit = true;
            }
            else if (Objects.equals(args[0], "--testCrypto")){
                var key = Util.randomStringGen(new Random(System.nanoTime()).nextInt(4,32));
                if (key.length() <= 16){
                    StringBuilder keyBuilder = new StringBuilder(key);
                    while (keyBuilder.length() < 16)
                        keyBuilder.append("0");
                    key = keyBuilder.toString();
                }
                else {
                    if (key.length() <= 32) {
                        StringBuilder keyBuilder = new StringBuilder(key);
                        while (keyBuilder.length() < 32)
                            keyBuilder.append("0");
                        key = keyBuilder.toString();
                    }
                }
                var data = Util.randomStringGen(16);

                try {
                    var dataEncrypted = encryptECB(data.getBytes(StandardCharsets.UTF_8),key.getBytes(StandardCharsets.UTF_8));
                    var dataDecrypted = decryptECB(dataEncrypted,key.getBytes(StandardCharsets.UTF_8));
                    String s = "\nkey:" + key + "\n" + "data:" + data +
                            "\n" +
                            "dataEncrypted:" + new String(dataEncrypted) +
                            "\ndataDecrypted:" + new String(dataDecrypted);
                    logger.info(s);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
        }

        logger.info("Hello World!");

        if (!new Util().fileExists(new Util().getWorkingDir() + File.separator + "config.json")){
            logger.warn("CONFIG NOT EXIST,creating.");
            try {
                if (!new Util().createFile(new Util().getWorkingDir() + File.separator + "config.json")){
                    logger.error("Unable to create file:" + new Util().getWorkingDir() + File.separator + "config.json");
                    System.exit(-1);
                }
                logger.info("Created Config,writing default config.");
                Gson gson = new GsonBuilder().serializeNulls().create();
                String cont = gson.toJson(new Configuration(50000,"Uranium"));
                File fp = new File(new Util().getWorkingDir() + File.separator + "config.json");
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
        File folder = new File(new Util().getWorkingDir() + File.separator + "clients");

        if (!folder.exists() && !folder.isDirectory()) {
            logger.warn("No Uranium sControl Client added.");
            folder.mkdirs();
            logger.warn("created Uranium sControl Client folder.");
            isInit = true;
        }
        if (isExampleGen) {
            try {
                logger.info("Generating Client Example.");
                Gson gson = new GsonBuilder().serializeNulls().create();
                String cont = gson.toJson(new SControlClient("mcdreforged",Util.randomStringGen(8),50010,"127.0.0.1",Util.randomStringGen(6)));
                File fp = new File(new Util().getWorkingDir() + File.separator + "clients" + File.separator + "example.json");
                FileOutputStream stream = new FileOutputStream(fp);
                OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                writer.append(cont);
                writer.close();
                stream.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        folder = new File(new Util().getWorkingDir() + File.separator + "whitelists");
        if (!folder.exists() && !folder.isDirectory()) {
            logger.warn("No Uranium whitelist added.");
            folder.mkdirs();
            logger.warn("created Uranium whitelist folder.");
            isInit = true;
        }
        if (isExampleGen) {
            try {
                logger.info("Generating Whitelist Example.");
                Gson gson = new GsonBuilder().serializeNulls().create();
                String[] players = {Util.randomStringGen(8),Util.randomStringGen(8),Util.randomStringGen(8),Util.randomStringGen(8),Util.randomStringGen(8)};
                String cont = gson.toJson(new Whitelist(players,Util.randomStringGen(8)));
                File fp = new File(new Util().getWorkingDir() + File.separator + "whitelists" + File.separator + "example.json");
                FileOutputStream stream = new FileOutputStream(fp);
                OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                writer.append(cont);
                writer.close();
                stream.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
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
        try {
            logger.info("Starting...");
            ServerSocket server = new ServerSocket(config.getPort());
            logger.info("Started Uranium qLink Service at "+ server.getLocalPort());
            while (true){
                try {
                    Socket socket = server.accept();
                    new ClientSocketHandler(socket);
                }
                catch (Exception e){
                    logger.warn("An error occurred while listening client connections:");
                    e.printStackTrace();
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Exit.");
    }
}
