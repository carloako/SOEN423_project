package replicas.carlo.serverside;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.registry.*;
import java.util.HashMap;

import javax.xml.ws.Endpoint;

public class Server extends Thread {

	private String city;
	private int serverPort;
	private int udpPort;
	private ServerOperationsInterface serverOps;
	private Endpoint endpoint;

	public Server(String city, int udpPort, int serverPort, ServerOperationsInterface db) throws Exception {

		this.city = city;
		this.serverPort = serverPort;
		this.udpPort = udpPort;
		this.serverOps = db;

		endpoint = Endpoint.publish("http://localhost:" + serverPort + "/" + city.toLowerCase(), serverOps);
		System.out.println(city + " web server is running");
	}
	
	public void run() {
		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(udpPort);
			System.out.println(city + "Server with server port " + serverPort + " ready and waiting ...");
			System.out.println(
					"Created a socket with port " + aSocket.getLocalPort() + " and host " + aSocket.getInetAddress());
			byte[] buffer = null;
			while (true) {
				buffer = new byte[1000];
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(request);
				String requestString = (new String(request.getData())).trim();
				String[] requestStringArr = requestString.split(" ");
				if (requestString.charAt(0) == 'A') {
					String eventType = null;
					if (requestStringArr[1].equals("Art")) {
						eventType = requestStringArr[1] + " " + requestStringArr[2];
					} else
						eventType = requestStringArr[1];
					String result = serverOps.listReservationSlotAvailableLocal(eventType);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'P') {
					String participantID = requestString.substring(2);
					String result = serverOps.getEventScheduleLocal(participantID);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'C') {
					String participantID = requestStringArr[1];
					String tbaEventStr = requestStringArr[2];
					String newEventType = requestStringArr[3];
					if (requestStringArr[3].equals("Art")) {
						newEventType = requestStringArr[3] + " " + requestStringArr[4];
					} else
						newEventType = requestStringArr[3];
					Event tbaEvent = serverOps.getEvent(tbaEventStr, newEventType);
					String result = null;
					if (tbaEvent != null) {
						boolean tbaEventContainsUser = tbaEvent.isUserBooked(participantID);
						if (tbaEventContainsUser) {
							result = "1 1";
						} else {
							result = "1 0";
						}
					} else {
						result = "0 0";
					}
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				} else if (requestString.charAt(0) == 'R') {
					String participantID = requestStringArr[1];
					String tbaEventStr = requestStringArr[2];
					String newEventType = "";
					if (requestStringArr[3].equals("Art")) {
						newEventType = requestStringArr[3] + " " + requestStringArr[4];
					} else
						newEventType = requestStringArr[3];
					String result = serverOps.reserveTicket(participantID, tbaEventStr, newEventType);
					byte[] m = result.getBytes();
					DatagramPacket reply = new DatagramPacket(m, result.length(), request.getAddress(),
							request.getPort());
					aSocket.send(reply);
				}
			}
		} catch (SocketException e) {
			System.out.println("Socket error");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (aSocket != null)
				aSocket.close();
		}
	}
	
	public void unpublishEndpoint() {
		endpoint.stop();
	}
}
