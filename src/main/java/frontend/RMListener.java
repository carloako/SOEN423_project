package frontend;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RMListener {
    private final int port;
    private final int messageLength;
    private final int sizeRMGroup;
    private int count;
    private final ReplicaResponse[] responses;
    private ReplicaResponse errorReplica;
    private final Logger logger = Logger.getLogger(RMListener.class.getName());

    public static RMListener build(int port) {
        return new RMListener(port, 100, 3);
    }

    public static RMListener build(int port, int sizeRMGroup) {
        return new RMListener(port, 100, sizeRMGroup);
    }

    RMListener(int port, int messageLength, int sizeRMGroup) {
        this.port = port;
        this.messageLength = messageLength;
        this.sizeRMGroup = sizeRMGroup;
        this.count = 0;
        this.responses = new ReplicaResponse[sizeRMGroup];
        this.errorReplica = null;
    }

    public void listen() throws SocketException {
        DatagramSocket udpSocket = new DatagramSocket(this.port);
        byte[] buffer = new byte[this.messageLength];

        while(this.count < this.sizeRMGroup) {
            logger.info("Waiting replica responses...");
            try {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(request);

                addResponse(request);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            System.out.println("Message received");
        }

        udpSocket.close();
    }

    protected void addResponse(DatagramPacket packet) {
        responses[count++] = new ReplicaResponse(packet);
    }

    public String extractResponse() {
        final String[] message = new String[1];
        Map<String, List<ReplicaResponse>> values = Arrays.stream(responses)
                .collect(Collectors.groupingBy(
                        ReplicaResponse::getMessage,
                        Collectors.toList()));

        values.forEach((value, content) -> {
            System.out.println(value + " " + content);
            if (content.size() >= 2) {
                message[0] = value;
            } else {
                this.errorReplica = content.get(0);
            }
        });

        System.out.println(message[0]);
        return message[0];
    }

    private final Map<String, Integer> ports = new HashMap<String, Integer>() {{
        put("/192.168.43.225", 8000);
        put("/192.168.43.203", 8001);
        put("/192.168.43.95", 8002);
    }};

    public void notifyRMBadResult(String originalMessage)  {
        if (errorReplica == null)
            return;

        String[] content = originalMessage.split("-");
        final String message = String.format("FE-%s-%d-Error", content[1], ports.getOrDefault(errorReplica.getAddress().toString(), 8000));
        System.out.println("Sending error message to RM");
        System.out.println(errorReplica.getAddress());
        System.out.println(errorReplica.getPort());
        Unicast.build(message, errorReplica.getAddress(), ports.get(errorReplica.getAddress().toString())).send();
//        try {
//            Unicast.build(message, InetAddress.getByName("localhost"), 8000).send();
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
    }
}
