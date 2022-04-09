package net.zhuruoling.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.zhuruoling.scontrol.SControlClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

public class Util {

    public boolean fileExists(String fileName){
        try {
            new FileReader(fileName);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public static String getWorkingDir(){
        File directory = new File("");
        return directory.getAbsolutePath();
    }

     public boolean createFile(String filePath) throws IOException {
         return new File(filePath).createNewFile();
     }

     public static String randomStringGen(int len){
        String ch = "abcdefghijklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0;i < len; i++){
            Random random = new Random(System.nanoTime());
            int num = random.nextInt(62);
            stringBuffer.append(ch.charAt(num));
        }
        return stringBuffer.toString();
     }
    private static byte[] encryptECB(byte[] data, byte[] key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
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
     public static void testCrypto(){
         final Logger logger = LoggerFactory.getLogger("Util");
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


     }

     public static void generateExample(){
         try {
             final Logger logger = LoggerFactory.getLogger("Util");
             logger.info("Generating Client Example.");
             Gson gson = new GsonBuilder().serializeNulls().create();
             String cont = gson.toJson(new SControlClient("mcdreforged",Util.randomStringGen(8),50010,"127.0.0.1",Util.randomStringGen(6),"",""));
             File fp = new File(Util.getWorkingDir() + File.separator + "clients" + File.separator + "example.json");
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

}
