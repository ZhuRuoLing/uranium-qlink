package net.ruogustudio.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public String getWorkingDir(){
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

}
