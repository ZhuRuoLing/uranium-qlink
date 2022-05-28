package net.zhuruoling.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.zhuruoling.EncryptedConnector;
import net.zhuruoling.message.MessageBuilderKt;
import net.zhuruoling.util.Result;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class SessionServer extends Thread {
    private Session session;
    private EncryptedConnector encryptedConnector = null;
    private Gson gson = new  GsonBuilder().serializeNulls().create();
    public SessionServer(Session session){

        this.setName("SessionInitialServer#" + this.getId());
        try {
            this.encryptedConnector = new EncryptedConnector(
                    new BufferedReader(
                            new InputStreamReader(this.session.socket.getInputStream())
                    ),
                    new PrintWriter(
                            new OutputStreamWriter(this.session.socket.getOutputStream())
                    ),
                    new String(
                            this.session.key
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.session = session;
    }

    @Override
    public void run() {
        String line = null;
        try {
            line = encryptedConnector.readLine();
        } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                line = encryptedConnector.readLine();
            } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendOK(String[] data){
        String content = "";
        if (Objects.isNull(data)){
            content = MessageBuilderKt.build(Result.OK, new String[]{});
        }
        else {
            content = MessageBuilderKt.build(Result.OK, data);
        }
        try {
            encryptedConnector.send(Objects.requireNonNull(content));
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }
}
