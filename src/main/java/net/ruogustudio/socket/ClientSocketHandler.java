package net.ruogustudio.socket;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.ruogustudio.EncryptedConnector;
import net.ruogustudio.client.Command;
import net.ruogustudio.client.Message;
import net.ruogustudio.configuration.ConfigReader;
import net.ruogustudio.scontrol.SControlClient;
import net.ruogustudio.scontrol.SControlClientFileReader;
import net.ruogustudio.util.Util;
import net.ruogustudio.whitelist.Whitelist;
import net.ruogustudio.whitelist.WhitelistReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.print.attribute.standard.MediaPrintableArea;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class ClientSocketHandler extends Thread {

    private final Socket client;
    private final EncryptedConnector encryptedConnector; //用这个类接下所有的socket
    private final Logger logger = LoggerFactory.getLogger("client-handler");
    Gson json = new GsonBuilder().serializeNulls().create();

    public ClientSocketHandler(Socket s) throws IOException {
        client = s;
        var in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        var out = new PrintWriter(client.getOutputStream(), true);
        this.encryptedConnector = new EncryptedConnector(in, out, Objects.requireNonNull(ConfigReader.read()).getCryptoKey());
        logger.info("client:" + client.getInetAddress() + ":" + client.getPort() + " connected.");
        this.start();
    }

    public void run() {
        try {
            String line = encryptedConnector.readLine(); //FIXME:json必须是一行的

            while (line != null) {
                logger.debug(line);
                try {
                    if (line.equals("DISCONNECT")) {
                        logger.debug("breaking thread");
                        break;
                    } else {
                        Gson gson = new Gson();
                        Command command = gson.fromJson(line, Command.class);
                        logger.info("Received command:" + command.getCmd() + " with load:" + Arrays.toString(command.getLoad()) + "from " + client.toString());
                        switch (command.getCmd()) {
                            case "LIST_SERVER":
                                var key = command.getLoad()[0];
                                if (!Objects.equals(key, ConfigReader.read().getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                if (new SControlClientFileReader().isNoClient()) {
                                    encryptedConnector.println(gson.toJson(new Message("NO_CLIENTS", new String[]{}), Message.class));
                                    break;
                                }
                                var scontrolList = new SControlClientFileReader().getClientList();
                                List<String> ls = new ArrayList<>();
                                scontrolList.forEach(scontrol -> {
                                    ls.add(scontrol.getName());
                                });
                                Message sm = new Message("OK", ls.toArray(new String[ls.size()]));

                                encryptedConnector.println(json.toJson(sm, Message.class));
                                break;
                            case "PING":
                                var _key_ = command.getLoad()[0];
                                if (!Objects.equals(_key_, Objects.requireNonNull(ConfigReader.read()).getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                encryptedConnector.println(gson.toJson(new Message("OK", new String[]{Objects.requireNonNull(ConfigReader.read()).getServerName()}), Message.class));
                                break;
                            case "GET_SERVER_STATUS":
                                if (!Objects.equals(command.getLoad()[1], Objects.requireNonNull(ConfigReader.read()).getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var sc = new SControlClientFileReader().read(command.getLoad()[0]);
                                var pingCmd = new Command("PING", new String[]{});
                                try {
                                    var sock = new Socket(sc.getIp(), sc.getPort());
                                    var conn = new EncryptedConnector(new BufferedReader(new InputStreamReader(sock.getInputStream()))
                                            , new PrintWriter(new OutputStreamWriter(sock.getOutputStream())),
                                            sc.getKey());
                                    conn.send(new GsonBuilder().serializeNulls().create().toJson(pingCmd, Command.class));
                                    var result = new GsonBuilder().serializeNulls().create().fromJson(conn.readLine(), Message.class);
                                    if (result.getMsg() == "OK") {
                                        encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                        break;
                                    }
                                } catch (Exception e) {
                                    encryptedConnector.println(gson.toJson(new Message("FAIL", new String[]{e.getMessage()}), Message.class));
                                    break;
                                }
                                break;
                            case "GET_SERVERS_STATUS":
                                if (!Objects.equals(command.getLoad()[1], Objects.requireNonNull(ConfigReader.read()).getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var lst = new SControlClientFileReader().getClientList();
                                var pingCmd1 = new Command("PING", new String[]{});
                                var resultList = new ArrayList<String>();
                                lst.forEach(sc1 -> {
                                    try {
                                        var sock1 = new Socket(sc1.getIp(), sc1.getPort());
                                        var conn = new EncryptedConnector(new BufferedReader(new InputStreamReader(sock1.getInputStream()))
                                                , new PrintWriter(new OutputStreamWriter(sock1.getOutputStream())),
                                                sc1.getKey());
                                        conn.send(new GsonBuilder().serializeNulls().create().toJson(pingCmd1, Command.class));
                                        var result = new GsonBuilder().serializeNulls().create().fromJson(conn.readLine(), Message.class);
                                        if (Objects.equals(result.getMsg(), "OK")) {
                                            resultList.add("OK");
                                        }
                                    } catch (Exception e) {
                                        resultList.add(e.getMessage());
                                    }
                                });
                                var ld = resultList.toArray(new String[resultList.size()]);
                                Message message = new Message("OK", ld);
                                encryptedConnector.send(json.toJson(message, message.getClass()));
                                break;
                            case "WHITELIST_QUERY":
                                var whitelistName = command.getLoad()[0];
                                var playerName = command.getLoad()[1];
                                var key_ = command.getLoad()[2];
                                if (playerName.contains("bot")) { //bot直接通过
                                    encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                    break;
                                }
                                if (!Objects.equals(key_, Objects.requireNonNull(ConfigReader.read()).getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var whitelistReader = new WhitelistReader();
                                if (whitelistReader.isNoWhitelist()) {
                                    encryptedConnector.println(gson.toJson(new Message("NO_WHITELIST", new String[]{}), Message.class));
                                    break;
                                }
                                var whitelist = whitelistReader.read(whitelistName);
                                if (whitelist == null) {
                                    encryptedConnector.println(gson.toJson(new Message("WHITELIST_NOT_EXIST", new String[]{}), Message.class));
                                    break;
                                }
                                if (whitelist.containsPlayer(playerName)) {
                                    encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                    break;
                                } else
                                    encryptedConnector.println(gson.toJson(new Message("NO_SUCH_PLAYER", new String[]{}), Message.class));
                                break;
                            case "WHITELIST_CREATE":
                                var name = command.getLoad()[0];
                                var key1 = command.getLoad()[1];
                                if (!Objects.equals(key1, ConfigReader.read().getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                if (!(new WhitelistReader().isFail()) && (new WhitelistReader().read(name) != null)) {
                                    encryptedConnector.println(gson.toJson(new Message("WHITELIST_EXISTS", new String[]{}), Message.class));
                                    break;
                                }
                                try {
                                    logger.info("Generating Whitelist " + name);
                                    Gson gson1 = new GsonBuilder().serializeNulls().create();
                                    String[] players = {};
                                    String cont = gson1.toJson(new Whitelist(players, name));
                                    File fp = new File(new Util().getWorkingDir() + File.separator + "whitelists" + File.separator + name + ".json");
                                    FileOutputStream stream = new FileOutputStream(fp);
                                    OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                                    writer.append(cont);
                                    writer.close();
                                    stream.close();
                                    encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                    break;
                                } catch (Exception e) {
                                    logger.error("An exception occurred:" + e.getMessage());
                                    e.printStackTrace();
                                    encryptedConnector.println("{\"msg\":\"INTERNAL_EXCEPTION\",\"load\":[]}");

                                }
                                break;
                            case "LAUNCH":
                                if (!Objects.equals(command.getLoad()[1], Objects.requireNonNull(ConfigReader.read()).getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var sc2 = new SControlClientFileReader().read(command.getLoad()[0]);
                                var command1 = new Command("PING", new String[]{});
                                boolean available = false;
                                try {
                                    var sock = new Socket(sc2.getIp(), sc2.getPort());
                                    var conn = new EncryptedConnector(new BufferedReader(new InputStreamReader(sock.getInputStream()))
                                            , new PrintWriter(new OutputStreamWriter(sock.getOutputStream())),
                                            sc2.getKey());
                                    conn.send(new GsonBuilder().serializeNulls().create().toJson(command1, Command.class));
                                    var result = new GsonBuilder().serializeNulls().create().fromJson(conn.readLine(), Message.class);
                                    if (Objects.equals(result.getMsg(), "OK")) {
                                        available = true;
                                    }
                                } catch (Exception ignored){}
                                if (available){
                                    var os = ManagementFactory.getOperatingSystemMXBean();
                                    if (os.getName().contains("Windows")){
                                        Runtime run = Runtime.getRuntime();
                                        Process process = run.exec("cmd /c start \"%s\"".formatted(sc2.getPath()),null, new File(sc2.getWorkingDir()));
                                        encryptedConnector.println(gson.toJson(new Message("OK", new String[]{Long.toString(process.pid())}), Message.class));
                                        break;
                                    }
                                    Runtime run = Runtime.getRuntime();
                                    Process process = run.exec(sc2.getPath(),null,new File(sc2.getWorkingDir()));
                                    encryptedConnector.println(gson.toJson(new Message("OK", new String[]{Long.toString(process.pid())}), Message.class));
                                }
                                else {
                                    encryptedConnector.println(gson.toJson(new Message("SERVER_ALREADY_RUNNING", new String[]{}), Message.class));
                                }
                                break;

                            case "WHITELIST_LIST":
                                if (!Objects.equals(command.getLoad()[0], ConfigReader.read().getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var whitelists = new WhitelistReader().getWhitelists();
                                String[] whitelistNames = new String[whitelists.size()];
                                for (int i = 0; i < whitelistNames.length; i++) {
                                    whitelistNames[i] = whitelists.get(i).getName();
                                }
                                Message msg = new Message("OK", whitelistNames);
                                encryptedConnector.println(gson.toJson(msg));
                                break;
                            case "WHITELIST_GET":
                                if (!Objects.equals(command.getLoad()[1], ConfigReader.read().getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                var wlName = command.getLoad()[0];
                                Whitelist wl = new WhitelistReader().read(wlName);
                                if (wl == null) {
                                    encryptedConnector.println(gson.toJson(new Message("NO_SUCH_WHITELIST", new String[]{}), Message.class));
                                    break;
                                }

                                encryptedConnector.println(gson.toJson(new Message("OK", new WhitelistReader().read(wlName).getPlayers())));
                                break;
                            case "WHITELIST_EDIT":
                                if (!Objects.equals(command.getLoad()[3], ConfigReader.read().getKey())) {
                                    encryptedConnector.println(gson.toJson(new Message("BAD_KEY", new String[]{}), Message.class));
                                    break;
                                }
                                String whiteName = command.getLoad()[0];
                                String operation = command.getLoad()[1];
                                String player = command.getLoad()[2];
                                if (new WhitelistReader().read(whiteName) == null) {
                                    encryptedConnector.println(gson.toJson(new Message("NO_SUCH_WHITELIST", new String[]{}), Message.class));
                                    break;
                                }
                                Whitelist whitelist1 = new WhitelistReader().read(whiteName);
                                switch (operation) {
                                    case "ADD":
                                        var before = whitelist1.getPlayers();
                                        String[] after = new String[before.length + 1];
                                        var beforeList1 = new ArrayList<String>(Arrays.stream(before).toList());
                                        if (beforeList1.contains(player)) {
                                            encryptedConnector.println(gson.toJson(new Message("PLAYER_ALREADY_EXISTS", new String[]{}), Message.class));
                                            return;
                                        }
                                        beforeList1.add(player);
                                        after = beforeList1.toArray(after);
                                        Whitelist newWhitelist = new Whitelist(after, whiteName);
                                        Gson gson1 = new GsonBuilder().serializeNulls().create();
                                        String cont = gson1.toJson(newWhitelist);
                                        File fp = new File(new Util().getWorkingDir() + File.separator + "whitelists" + File.separator + whiteName + ".json");
                                        FileOutputStream stream = new FileOutputStream(fp);
                                        OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
                                        writer.append(cont);
                                        writer.close();
                                        stream.close();
                                        encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                        break;
                                    case "REMOVE":
                                        String[] before1 = whitelist1.getPlayers();
                                        String[] after1 = new String[before1.length - 1];
                                        var beforeList = new ArrayList<String>(Arrays.stream(before1).toList());
                                        if (!beforeList.contains(player)) {
                                            encryptedConnector.println(gson.toJson(new Message("NO_SUCH_PLAYER", new String[]{}), Message.class));
                                            break;
                                        }
                                        beforeList.remove(player);
                                        after1 = beforeList.toArray(after1);
                                        Whitelist whitelist2 = new Whitelist(after1, whiteName);
                                        Gson gson2 = new GsonBuilder().serializeNulls().create();
                                        String s = gson2.toJson(whitelist2);
                                        File file = new File(new Util().getWorkingDir() + File.separator + "whitelists" + File.separator + whiteName + ".json");
                                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                                        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
                                        outputStreamWriter.append(s);
                                        outputStreamWriter.close();
                                        fileOutputStream.close();
                                        encryptedConnector.println(gson.toJson(new Message("OK", new String[]{}), Message.class));
                                        break;
                                    default:
                                        encryptedConnector.println(gson.toJson(new Message("BAD_OPERATION", new String[]{}), Message.class));
                                        break;
                                }

                            default:
                                break;
                        }
                    }

                } catch (Exception e) {
                    logger.warn("An error occurred.");
                    e.printStackTrace();
                }
                line = encryptedConnector.readLine();
            }
            logger.info("client:" + client.getInetAddress() + ":" + client.getPort() + " disconnected.");
            client.close();
        } catch (Exception ignored) {
            ignored.printStackTrace();
            logger.warn("Client(" + client.toString() + ") closed a connection.");
        }
    }

}
