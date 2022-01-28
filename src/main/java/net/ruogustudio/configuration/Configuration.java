package net.ruogustudio.configuration;

import com.google.gson.annotations.SerializedName;
import net.ruogustudio.util.Util;

public class Configuration {
    public Configuration(int port,String serverName,String key,String cryptoKey){
        this.port = port;
        this.serverName = serverName;
        this.cryptoKey = cryptoKey;
        this.key = key;
    }
    public Configuration(int port,String serverName){
        this.port = port;
        this.serverName = serverName;
        this.cryptoKey = new Util().randomStringGen(16);
        this.key = new Util().randomStringGen(16);
    }
    @SerializedName("port")
    public int port;
    @SerializedName("server_name")
    public String serverName;
    @SerializedName("key")
    public String key;
    @SerializedName("crypto_key")
    public String cryptoKey;

    public int getPort() {
        return port;
    }

    public String getCryptoKey() {
        return cryptoKey;
    }

    public String getKey() {
        return key;
    }

    public String getServerName() {
        return serverName;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setCryptoKey(String cryptoKey) {
        this.cryptoKey = cryptoKey;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

}
