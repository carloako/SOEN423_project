package frontend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Unicast {
    String message;
    InetAddress udpClientHost;
    int port;

    public static Unicast build(String message, InetAddress host, int port) {
        return new Unicast(message, host, port);
    }

    public static InetAddress createAddress(byte[] ip) throws UnknownHostException { return InetAddress.getByAddress(ip); }
    public static InetAddress createAddress(String hostName) throws UnknownHostException { return InetAddress.getByName(hostName); }

    private Unicast(String message, InetAddress host, int port) {
        this.message = message;
        this.udpClientHost = host;
        this.port = port;
    }

    public boolean send() {
        try {
            DatagramSocket udpClient = new DatagramSocket();

            byte[] udpBytesRequest = message.getBytes();
            DatagramPacket request = new DatagramPacket(
                    udpBytesRequest,
                    message.length(),
                    udpClientHost,
                    port
            );

            udpClient.send(request);

//            byte[] udpBytesReply = new byte[10000];
//            DatagramPacket reply = new DatagramPacket(udpBytesReply, udpBytesReply.length);
//
//            udpClient.receive(reply);
//            return Boolean.parseBoolean(new String(reply.getData()).trim());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
