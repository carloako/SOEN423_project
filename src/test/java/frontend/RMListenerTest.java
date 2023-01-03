package frontend;

import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;

class RMListenerTest {

    @Test
    void testExtractResponse() {
        RMListener listener = RMListener.build(1234);
        byte[] message1 = "message1".getBytes();
        byte[] message2 = "message2".getBytes();
        listener.addResponse(new DatagramPacket(message1, message1.length));
        listener.addResponse(new DatagramPacket(message1, message1.length));
        listener.addResponse(new DatagramPacket(message2, message2.length));

        String message = listener.extractResponse();

        System.out.println(message);
    }

}
