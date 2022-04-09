package net.zhuruoling.broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;

public class UdpBroadcastReceiver extends Thread{
    private static final Logger logger = LoggerFactory.getLogger("UdpBroadcastReceiver");

    public UdpBroadcastReceiver(){
        this.setName("UdpBroadcastReceiver#" + getId());
    }

    @Override
    public void run() {
        try {
            int port = 10086;
            String address = "224.114.51.4"; // 224.114.51.4:10086
            MulticastSocket socket = null;
            InetAddress inetAddress = null;
            inetAddress = InetAddress.getByName(address);
            socket = new MulticastSocket(port);
            logger.info("Started Broadcast Receiver at " + address + ":" + port);
            socket.joinGroup(new InetSocketAddress(inetAddress,port), NetworkInterface.getByInetAddress(inetAddress));

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            for (;;){
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(),
                        packet.getLength());

                var broadcast = BroadcastBuilderKt.buildFromJson(msg);
                logger.info("Received Broadcast:" + broadcast.getContent());
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
