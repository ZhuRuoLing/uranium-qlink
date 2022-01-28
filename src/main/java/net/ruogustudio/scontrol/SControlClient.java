package net.ruogustudio.scontrol;

import com.google.gson.annotations.SerializedName;

public class SControlClient {
    public SControlClient(String type, String name, int port, String ip, String key){
        this.type = type;
        this.ip = ip;
        this.port = port;
        this.name = name;
        this.key = key;
    }
    @SerializedName("type")
    String type = "";
    @SerializedName("ip")
    String ip = "";
    @SerializedName("port")
    int port = 0;
    @SerializedName("name")
    String name;
    @SerializedName("key")
    String key;

    public String getName() {
        return name;
    }

    public int getPort() {
        return port;
    }

    public String getType() {
        return type;
    }

    public String getIp() {
        return ip;
    }

    public String getKey() {
        return key;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SControlClient{" +
                "type='" + type + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", name='" + name + '\'' +
                '}';
    }
}
