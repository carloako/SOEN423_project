package frontend;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;

public class ReplicaResponse {
    private final String message;
    private final InetAddress address;
    private final int port;
    private final SocketAddress socketAddress;

    public ReplicaResponse(DatagramPacket packet) {
        this.message = formatMessage(packet);
        this.address = packet.getAddress();
        this.port = packet.getPort();
        this.socketAddress = packet.getSocketAddress();
    }

    private String formatMessage(DatagramPacket packet) {
        return new String(Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength())).trim() + "";
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getMessage() {
        return message;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }
}
