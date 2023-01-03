package replicamanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import replicas.philippe.Client.AdminClient;
import replicas.philippe.Client.ParticipantClient;
import replicas.philippe.Server.Admin;
import replicas.philippe.Server.FestivalServer;
import replicas.philippe.Server.Participant;
import replicas.philippe.Server.ParticipantImpl;
import replicas.philippe.model.Event;
import replicas.philippe.model.EventType;
import replicas.philippe.model.Response;

public class ReplicaManager2 extends Thread {

	static Map<String, Integer> socketPorts = new HashMap<String, Integer>() {
		{
			put("MTL", 1110);
			put("TOR", 1111);
			put("VAN", 111);
		}
	};
	static Map<String, Integer> ports = new HashMap<String, Integer>() {
		{
			put("MTL", 8080);
			put("TOR", 8081);
			put("VAN", 808);
		}
	};

	static int rmPort = Address.RM2_PORT;
	static int mtlUDPPort = socketPorts.get("MTL");
	static int mtlWServerPort = ports.get("MTL");
	static int torUDPPort = socketPorts.get("TOR");
	static int torWServerPort = ports.get("TOR");
	static int vanUDPPort = socketPorts.get("VAN");
	static int vanWServerPort = ports.get("VAN");

	private static int bufferIndex = 1;
	private static TreeMap<Integer, Request> requestBuffer = new TreeMap<Integer, Request>();
	private static boolean cont = true;

	public static void main(String[] args) throws Exception {

		InetAddress sqAddress = InetAddress.getByName(Address.SQ_ADDRESS);
		int sqPort = Address.SQ_PORT;

		InetAddress feAddress = InetAddress.getByName(Address.FE_ADDRESS);
		int fePort = Address.FE_PORT;

		String mtl = "MTL";
		Map<String, Map<String, Event>> mtlEventRecords = createDB(mtl);
		FestivalServer mtlServer = new FestivalServer(mtl, mtlEventRecords);
		mtlServer.start();
		String tor = "TOR";
		Map<String, Map<String, Event>> torEventRecords = createDB(tor);
		FestivalServer torServer = new FestivalServer(tor, torEventRecords);
		torServer.start();
		String van = "VAN";
		Map<String, Map<String, Event>> vanEventRecords = createDB(van);
		FestivalServer vanServer = new FestivalServer(van, vanEventRecords);
		vanServer.start();

		DatagramSocket aSocket = null;
		try {
			aSocket = new DatagramSocket(rmPort);
			System.out.println("RM1 with server port " + rmPort + " ready and waiting ...");
			System.out.println(
					"Created a socket with port " + aSocket.getLocalPort() + " and host " + aSocket.getInetAddress());
			System.out.println("============================");
			byte[] buffer = null;
			while (true) {
				System.out.println("----------------------------");
				buffer = new byte[1000];
				DatagramPacket dgram = new DatagramPacket(buffer, buffer.length);
				aSocket.receive(dgram);
				String msg = (new String(dgram.getData())).trim();
				System.out.println("Received message from " + dgram.getAddress() + ": " + dgram.getPort());
				System.out.println("Message: " + msg);
				String[] temp = msg.split("-");
				System.out.println(temp[0]);
				String[] msgArr = Arrays.copyOfRange(temp, 1, temp.length);
				String senderName = null;

				sendPacket(dgram.getAddress(), dgram.getPort(), "0");

//				if (dgram.getPort() == sqPort) {
				if (temp[0].equals("seq")) {
					System.out.println("Putting msg to buffer");
					String firstHalf = msg.substring(0, msg.lastIndexOf('-'));
					String firstHalfArr[] = firstHalf.split("-");
					String secondHalf = msg.substring(msg.lastIndexOf('-'));
					String secondHalfArr[] = secondHalf.split(",");

//					String sqNum = firstHalfA rr[1];
					String sqNum = msgArr[3];
					System.out.println("sqNum = " + sqNum);
					String params[] = secondHalfArr;

//					Request newReq = new Request(msg, dgram);
					Request newReq = new Request(msg.substring(msg.indexOf("-") + 1), dgram);
					requestBuffer.put(Integer.parseInt(sqNum), newReq); // TODO do we need buffer, what impementation

					runBuffer();

//				} else if (dgram.getPort() == fePort) {
				} else if (temp[0].equals("FE")) {
//					String temp[] = msg.split("-");
					String city = temp[1];
					int recvPort = Integer.parseInt(temp[2]);
					// TODO how to replace Replica Manager
					if (rmPort == recvPort) {
						InetAddress rm1Add = null;
						InetAddress rm2Add = null;
						int port1 = 0;
						int port2 = 0;
						if (rmPort == 8000) {
							port1 = Address.RM2_PORT;
							rm1Add = InetAddress.getByName(Address.RM2_ADDRESS);
							port2 = Address.RM3_PORT;
							rm2Add = InetAddress.getByName(Address.RM3_ADDRESS);
						} else if (rmPort == 8001) {
							port1 = Address.RM1_PORT;
							rm1Add = InetAddress.getByName(Address.RM1_ADDRESS);
							port2 = Address.RM3_PORT;
							rm2Add = InetAddress.getByName(Address.RM3_ADDRESS);
						} else if (rmPort == 8002) {
							port1 = Address.RM1_PORT;
							rm1Add = InetAddress.getByName(Address.RM1_ADDRESS);
							port2 = Address.RM2_PORT;
							rm2Add = InetAddress.getByName(Address.RM2_ADDRESS);
						} else {
							System.out.println("Invalid id");
							System.exit(0);
						}
						if (city.equals("MTL")) {
							mtlServer = new FestivalServer(mtl, mtlEventRecords);
						} else if (city.equals("TOR")) {
							torServer = new FestivalServer(tor, torEventRecords);
						} else if (city.equals("VAN")) {
							vanServer = new FestivalServer(van, vanEventRecords);
						} else {
							System.out.println("Server reset error");
						}
//						sendPacket(rm1Add, port1, "success");
//						sendPacket(rm2Add, port2, "success");
//						sendPacket(dgram.getAddress(), dgram.getPort(), "succes");
					} else {
						buffer = new byte[1000];
						DatagramPacket checkServerDgram = new DatagramPacket(buffer, buffer.length);
						aSocket.receive(checkServerDgram);
					}
				} else {
					System.out.println("Sender name error");
				}
			}
		} catch (SocketException e) {
			System.out.println("RM Socket exception");
			e.printStackTrace();
		}
	}

	public static void runBuffer() {
		System.out.println("Running buffer");
		System.out.println(bufferIndex + " == " + requestBuffer.firstKey());
		while (!requestBuffer.isEmpty() && bufferIndex == requestBuffer.firstKey()) {
			Request req = requestBuffer.remove(requestBuffer.firstKey());
			String msg = req.getMsg();
			System.out.println("Processing msg: " + msg);
			DatagramPacket dgram = req.getDgram();

			String[] msgContent = msg.split("-");

			String firstHalf = msg.substring(0, msg.lastIndexOf('-'));
			String firstHalfArr[] = firstHalf.split("-");
			String secondHalf = msg.substring(msg.lastIndexOf('-') + 1);
			String secondHalfArr[] = secondHalf.split(",");

			String sqNum = firstHalfArr[1];
//			String params[] = secondHalfArr;
			String[] params = msgContent[2].split(",");

			String methodString = firstHalfArr[0];
//			int method = Integer.parseInt(methodString); // TODO convert methodString to int

			// TODO switch case
			String defaultCity = "MTL";
			String userID = null; // TODO pass userID for 4, 5, 6, 7
			String eventType = null;
			String eventID = null;
			Response serverResponse = null;
			String returnVal = null; // TODO
			System.out.println("Starting process");
			try {
				Participant serverP = null;
				Admin serverA = null;
//				switch (method) {
				switch (methodString) {
//				case 1:
				case "addReservationSlot":
					System.out.println("Adding reservation slot:");
					eventID = params[0];
					eventType = params[1];
					int capacity = Integer.parseInt(params[2]);

					System.out.println(eventType + ", " + eventID + ", " + capacity);
					System.out.println(getCity(eventID));

					serverA = AdminClient.createAdmin(getCity(eventID));

					serverResponse = serverA.addReservationSlot(eventID, eventType, capacity);

					if (serverResponse.isSuccessful())
						returnVal = "1";
					else
						returnVal = "0";
//					returnVal = serverResponse;
					break;
//				case 2:
				case "removeReservationSlot":
					System.out.println("Removing reservation slot:");
					eventID = params[0];
					eventType = params[1];

					serverA = AdminClient.createAdmin(getCity(eventID));

					serverResponse = serverA.removeReservationSlot(eventID, eventType);
					if (serverResponse.isSuccessful())
						returnVal = "1";
					else
						returnVal = "0";
//					returnVal = serverResponse;
					break;
//				case 3:
				case "listReservationSlotAvailable":
					System.out.println("Listing available reservation slot: ");
					eventType = params[0];

					serverA = AdminClient.createAdmin(defaultCity);

					serverResponse = serverA.listReservationSlotAvailable(eventType);

					String slots = serverResponse.getMessage().trim();
					if (!slots.isEmpty()) {
						returnVal = "1:";
						String temp[] = serverResponse.getMessage().split(",");
						for (int i = 0; i < temp.length; i++) {
							temp[i] = temp[i].trim();
						}
						Arrays.sort(temp);
						for (int i = 0; i < temp.length; i++) {
							returnVal += temp[i].trim();
							if (i != temp.length - 1)
								returnVal += ",";
						}
					} else
						returnVal = "0";
					System.out.println(serverResponse.getMessage());
					break;
//				case 4:
				case "reserveTicket":
					System.out.println("Reserving ticket: " + Arrays.toString(params));
					userID = params[0];
					eventID = params[1];
					eventType = params[2];

					serverP = ParticipantClient.createParticipant(getCity(eventID));

					serverResponse = serverP.reserveTicket(userID, eventID, eventType);
					if (serverResponse.isSuccessful())
						returnVal = "1";
					else
						returnVal = "0";
//					returnVal = serverResponse;
					break;
//				case 5:
				case "getEventSchedule":
					System.out.println("Getting event schedule: ");
					userID = params[0]; // TODO need user ID here

					serverP = ParticipantClient.createParticipant(getCity(userID));

					serverResponse = serverP.getEventSchedule(userID);
//					returnVal = serverResponse;

					String schedule = serverResponse.getMessage().trim();
					if (!schedule.isEmpty()) {
						returnVal = "1:";
						String temp[] = serverResponse.getMessage().split(",");
						for (int i = 0; i < temp.length; i++) {
							temp[i] = temp[i].trim();
						}
						Arrays.sort(temp);
						for (int i = 0; i < temp.length; i++) {
							returnVal += temp[i].trim();
							if (i != temp.length - 1)
								returnVal += ",";
						}
					} else
						returnVal = "0";
					System.out.println(serverResponse.getMessage());
					break;
//				case 6:
				case "cancelTicket":
					System.out.println("Cancelling ticket: ");
					userID = params[0];
					eventID = params[1];

					serverP = ParticipantClient.createParticipant(getCity(eventID));

					serverResponse = serverP.cancelTicket(userID, eventID);
					if (serverResponse.isSuccessful())
						returnVal = "1";
					else
						returnVal = "0";
//					returnVal = serverResponse;
					break;
//				case 7:
				case "exchangeTickets":
					System.out.println("Exchanging ticket: ");
					System.out.println("params[0] = " + params[0]);
					userID = params[0];
					String tbcEventID = params[1];
					String tbaEventID = params[2];
					eventType = params[3];

					serverP = ParticipantClient.createParticipant(getCity(tbcEventID));

					serverResponse = serverP.exchangeTickets(userID, tbcEventID, tbaEventID, eventType);
					if (serverResponse.isSuccessful())
						returnVal = "1";
					else
						returnVal = "0";
//					returnVal = serverResponse;
					break;
				}
				bufferIndex++;
//				sendPacket(dgram.getAddress(), dgram.getPort(), returnVal);
				sendPacket(InetAddress.getByName("192.168.43.203"), 8999, returnVal == null ? "" : returnVal);
//				sendPacket(InetAddress.getByName("localhost"), 8999, returnVal == null ? "" : returnVal);
			} catch (Exception e) {
				System.out.println("Replica throws exception");
				e.printStackTrace();
			}
		}
	}

	public static void sendPacket(InetAddress address, int port, String msg) {
		int fePort = 0; // TODO front end port
		DatagramSocket aSocket = null;

		System.out.println("Sending to " + address + ": " + port);
		try {
			aSocket = new DatagramSocket(fePort);
			byte[] m = msg.getBytes();
			DatagramPacket reply = new DatagramPacket(m, msg.length(), address, port);
			System.out.println(aSocket.getLocalPort());
			aSocket.send(reply);
			System.out.println(aSocket.getLocalPort());
		} catch (SocketException e) {
			System.out.println("Socket-to-FE Socket exception");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Socket-to-FE IO exception");
			e.printStackTrace();
		}
	}

	private static String getCity(String data) {
		String city = data.substring(0, 3);
		return city;
	}

	private static Map<String, Map<String, Event>> createDB(String city) throws Exception {
		String fest1ID = city + "M010122";
		Event fest1 = new Event(10, EventType.ART_GALLERY.value);
		String fest2ID = city + "M020122";
		Event fest2 = new Event(20, EventType.ART_GALLERY.value);
		String fest3ID = city + "A030122";
		Event fest3 = new Event(30, EventType.ART_GALLERY.value);
		String fest4ID = city + "E040122";
		Event fest4 = new Event(40, EventType.ART_GALLERY.value);
		String fest5ID = city + "E050122";
		Event fest5 = new Event(50, EventType.ART_GALLERY.value);
		HashMap<String, Event> sampleF1 = new HashMap<String, Event>() {
			{
				put(fest1ID, fest1);
				put(fest2ID, fest2);
				put(fest3ID, fest3);
				put(fest4ID, fest4);
				put(fest5ID, fest5);
			}
		};

		String fest6ID = city + "M060122";
		Event fest6 = new Event(10, EventType.CONCERT.value);
		String fest7ID = city + "M070122";
		Event fest7 = new Event(20, EventType.CONCERT.value);
		String fest8ID = city + "A080122";
		Event fest8 = new Event(30, EventType.CONCERT.value);
		HashMap<String, Event> sampleF2 = new HashMap<String, Event>() {
			{
				put(fest6ID, fest6);
				put(fest7ID, fest7);
				put(fest8ID, fest8);
			}
		};

		String fest9ID = city + "M090122";
		Event fest9 = new Event(10, EventType.THEATRE.value);
		String fest10ID = city + "M100122";
		Event fest10 = new Event(20, EventType.THEATRE.value);
		String fest11ID = city + "A110122";
		Event fest11 = new Event(30, EventType.THEATRE.value);
		HashMap<String, Event> sampleF3 = new HashMap<String, Event>() {
			{
				put(fest9ID, fest9);
				put(fest10ID, fest10);
				put(fest11ID, fest11);
			}
		};

		Map<String, Map<String, Event>> sampleDB = new HashMap<String, Map<String, Event>>() {
			{
				put(EventType.ART_GALLERY.value, sampleF1);
				put(EventType.CONCERT.value, sampleF2);
				put(EventType.THEATRE.value, sampleF3);
			}
		};

		return sampleDB;
	}
}
