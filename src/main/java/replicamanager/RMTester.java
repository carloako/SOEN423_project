package replicamanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class RMTester {

	private static DatagramSocket aSocket;

	public static void main(String args[]) {
		try {
//			String msgs[] = { "1-0-Art Gallery,MTLM050522,50", "1-1-Theatre,TORM050522,50", "1-2-Theatre,VANM050522,50",
//					"3-3-Theatre" }; // add slots
//			String msgs[] = {"2-0-Theatre,MTLM090122", "2-1-Theatre,TORM090122", "2-2-Theatre,VANM090122", "3-3-Theatre", "2-4-Theatre,MTLM090122"}; // remove slots
//			String msgs[] = {"4-0-Theatre,MTLM090122,MTLA1234", "4-1-Art Gallery,TORM010122,MTLA1234", "4-2-Concerts,VANM060122,MTLA1234", "5-3-MTLA1234"
//					,"6-4-MTLM090122,MTLA1234", "6-5-TORM010122,MTLA1234", "6-6-VANM060122,MTLA1234", "5-7-MTLA1234"}; // reserve and cancel
//			String msgs[] = {"4-0-Theatre,MTLM090122,MTLA1234", "4-1-Art Gallery,MTLM010122,MTLA1234", "4-2-Concerts,MTLM060122,MTLA1234", "5-3-MTLA1234"
//					,"6-4-MTLM090122,MTLA1234", "6-5-MTLM010122,MTLA1234", "6-6-MTLM060122,MTLA1234", "5-7-MTLA1234"}; // reserve and cancel
//			String msgs[] = {"3-3-Art Gallery", "3-1-Art Gallery", "2-2-Art Gallery,MTLM050522", "1-0-Art Gallery,MTLM050522,50"};
//			String msgs[] = { "4-0-Art Gallery,MTLM010122,MTLA1234", "5-1-MTLA1234" , "6-2-MTLM010122,MTLA1234", "5-3-MTLA1234"};
			String msgs[] = { "4-0-Art Gallery,MTLM010122,MTLA1234", "5-1-MTLA1234" , "7-2-Art Gallery,MTLM010122,MTLM020122,MTLA1234", "5-3-MTLA1234"
					,"7-4-Theatre,MTLM020122,VANM090122,MTLA1234", "5-5-MTLA1234", "7-6-Concerts,VANM090122,TORM060122,MTLA1234", "5-7-MTLA1234"}; // exchange
//			String msgs[] = { "4-0-Art Gallery,MTLM010122,MTLA1234", "5-1-MTLA1234" , "7-2-Art Gallery,MTLM010122,MTLM020122,MTLA1234", "5-3-MTLA1234"
//					,"7-4-Theatre,MTLM020122,VANM090122,MTLA1234", "5-5-MTLA1234", "7-6-Concerts,VANM090122,TORM060122,MTLA1234", "5-7-MTLA1234"};
//			String msgs[] = {"3-0-Theatre"};
//			String msgs[] = {"4-0-Theatre,MTLM090122,MTLA1234", "5-1-MTLA1234", "6-2-MTLM090122,MTLA1234"};
//			String msgs[] = {"2-0-Theatre,MTLM090122"};
//			String msgs = "Theatre";
			aSocket = new DatagramSocket(9000);
			byte[] buffer = null;
			for (int i = 0; i < msgs.length; i++) {
//				String msg = "testing udp port";
//				String msg = "3-" + i + "-" + msgs;
				String msg = msgs[i];
//				String msg = "8000-mtl";
				sendPacket(msg, InetAddress.getByName("localhost"), 8000);
//				sendPacket(msg, InetAddress.getByName("localhost"), 8001);
//				sendPacket(msg, InetAddress.getByName("localhost"), 8002);

//				aSocket.send(request2);

				buffer = new byte[1000];

				buffer = new byte[1000];
//				DatagramPacket reply2 = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("localhost"), 8001);
//				aSocket.receive(reply2);

				String r1 = recvPacket();
//				String r2 = recvPacket();
//				String r3 = recvPacket();

//				System.out.println("r1 == r2" + r1.equals(r2));
//				System.out.println("r1 == r3" + r1.equals(r3));
//				System.out.println("r2 == r3" + r2.equals(r3));

//				System.out.println("Received messaged from " + reply2.getAddress() + ": " + reply2.getPort());
//				String responseMsg2 = (new String(reply2.getData())).trim();
//				System.out.println(responseMsg2);
//				if (responseMsg.equals(responseMsg2)) {
//					System.out.println("TRUE");
//				} else
//					System.out.println("FALSE");
			}
//			for (int i = 0; i < msgs.length; i++) {
//				
//			}
			aSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void sendPacket(String msg, InetAddress add, int port) throws IOException {
		byte[] buffer = msg.getBytes();
		DatagramPacket request = new DatagramPacket(buffer, msg.length(), add, port);
//		DatagramPacket request2 = new DatagramPacket(buffer, msg.length(), InetAddress.getByName("localhost"),
//				8001);
		System.out.println("Sending to " + request.getAddress() + ": " + request.getPort());
		aSocket.send(request);
	}

	public static String recvPacket() throws IOException {
		byte[] buffer = new byte[1000];
		DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
		aSocket.receive(reply);
		System.out.println("Received messaged from " + reply.getAddress() + ": " + reply.getPort());
		String responseMsg = (new String(reply.getData())).trim();
		System.out.println(responseMsg);
		return responseMsg;
	}
}
