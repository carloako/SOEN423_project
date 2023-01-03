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
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import replicas.carlo.serverside.Event;
import replicas.carlo.serverside.Server;
import replicas.carlo.serverside.ServerOperationsImpl;
import replicas.carlo.serverside.ServerOperationsInterface;

public class ReplicaManager1 extends Thread {

	static int rmPort = Address.RM1_PORT; // TODO for other rms
	static int mtlUDPPort = 5000;
	static int mtlWServerPort = 6000;
	static int torUDPPort = 5001;
	static int torWServerPort = 6001;
	static int vanUDPPort = 5002;
	static int vanWServerPort = 6002;

	private static int bufferIndex = 1; // Starts with 1 since we will process the first process
	private static TreeMap<Integer, Request> requestBuffer = new TreeMap<Integer, Request>();
	private static boolean cont = true;

	public static void main(String[] args) throws Exception {

		InetAddress sqAddress = InetAddress.getByName(Address.SQ_ADDRESS); // TODO address of sq and fe
		int sqPort = Address.SQ_PORT;
		InetAddress feAddress = InetAddress.getByName(Address.FE_ADDRESS);
		int fePort = Address.FE_PORT;

		String mtl = "MTL";
		ServerOperationsImpl mtlDB = createDB(mtl);
//		mtlDB.setToFail(true);
		Server mtlServer = new Server(mtl, mtlUDPPort, mtlWServerPort, mtlDB);
		mtlServer.start();
		String tor = "TOR";
		ServerOperationsImpl torDB = createDB(tor);
		Server torServer = new Server(tor, torUDPPort, torWServerPort, torDB);
		torServer.start();
		String van = "VAN";
		ServerOperationsImpl vanDB = createDB(van);
		Server vanServer = new Server(van, vanUDPPort, vanWServerPort, vanDB);
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
				System.out.println("Received message from " + dgram.getAddress() + ": " + dgram.getPort() + " == " + fePort);
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

//					String sqNum = firstHalfArr[1];
					String sqNum = msgArr[3];
					System.out.println("sqNum = " + sqNum);
//					String params[] = secondHalfArr;
					String params[] = msgArr[1].split(",");

//					Request newReq = new Request(msg, dgram);
					Request newReq = new Request(msg.substring(msg.indexOf("-") + 1), dgram);
					requestBuffer.put(Integer.parseInt(sqNum), newReq); // TODO do we need buffer, what impementation

					runBuffer();

//				} else if (dgram.getPort() == fePort) {
				} else if (temp[0].equals("FE")) {
//					String temp[] = msg.split("-");
					String city = temp[1];
					int recvPort = Integer.parseInt(temp[2]);
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
							mtlServer.unpublishEndpoint();
							mtlServer = new Server("MTL", mtlUDPPort, mtlWServerPort, mtlDB);
						} else if (city.equals("TOR")) {
							torServer.unpublishEndpoint();
							torServer = new Server("TOR", torUDPPort, torWServerPort, torDB);
						} else if (city.equals("VAN")) {
							vanServer.unpublishEndpoint();
							vanServer = new Server("VAN", vanUDPPort, vanWServerPort, vanDB);
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
		} finally {
			if (aSocket != null)
				aSocket.close();
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

			String city = msgContent[1];
			// TODO switch case
			String defaultCity = "MTL";
			String userID = null;
			String eventType = null;
			String eventID = null;
			String serverResponse = null;
			String returnVal = null;
			System.out.println("Starting process");
			try {
				ServerOperationsInterface server = null;
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

					server = getServer(getCity(eventID));

					serverResponse = server.addReservationSlot(eventID, eventType, capacity);
					returnVal = serverResponse;
					break;
//				case 2:
				case "removeReservationSlot":
					System.out.println("Removing reservation slot:");
					eventID = params[0];
					eventType = getEventType(params[1]);

					server = getServer(getCity(eventID));

					serverResponse = server.removeReservationSlot(eventID, eventType);
					returnVal = serverResponse;
					break;
//				case 3:
				case "listReservationSlotAvailable":
					System.out.println("Listing available reservation slot: ");
					eventType = getEventType(params[0]);

					server = getServer(defaultCity);

					serverResponse = server.listReservationSlotAvailable(eventType);
					returnVal = serverResponse;
					System.out.println(returnVal);
					break;
//				case 4:
				case "reserveTicket":
					System.out.println("Reserving ticket: " + Arrays.toString(params));
					userID = params[0];
					eventID = params[1];
					eventType = getEventType(params[2]);

					server = getServer(getCity(eventID));

					serverResponse = server.reserveTicket(userID, eventID, eventType);
					returnVal = serverResponse;
					System.out.println(returnVal);
					break;
//				case 5:
				case "getEventSchedule":
					System.out.println("Getting event schedule: ");
					userID = params[0];

					server = getServer(getCity(userID));

					serverResponse = server.getEventSchedule(userID);
					returnVal = serverResponse;
					break;
//				case 6:
				case "cancelTicket":
					System.out.println("Cancelling ticket: ");
					userID = params[0];
					eventID = params[1];

					server = getServer(getCity(eventID));

					serverResponse = server.cancelTicket(userID, eventID);
					returnVal = serverResponse;
					break;
//				case 7:
				case "exchangeTickets":
					System.out.println("Exchanging ticket: ");
					System.out.println("params[0] = " + params[0]);
					userID = params[0];
					String tbcEventID = params[1];
					String tbaEventID = params[2];
					eventType = getEventType(params[3]);

					server = getServer(getCity(tbcEventID));

					serverResponse = server.exchangeTickets(userID, tbcEventID, tbaEventID, eventType);
					returnVal = serverResponse;
					break;
				}
				bufferIndex++;
//				sendPacket(dgram.getAddress(), dgram.getPort(), returnVal == null ? "" : returnVal);
				sendPacket(InetAddress.getByName(Address.FE_ADDRESS), 8999, returnVal == null ? "" : returnVal);
//				sendPacket(InetAddress.getByName("localhost"), 8999, returnVal == null ? "" : returnVal);
			} catch (Exception e) {
				System.out.println("Replica throws exception");
				e.printStackTrace();
			}
		}
	}

	public static ServerOperationsInterface getServer(String city) throws MalformedURLException {
		String wsPort = getWSPort(city);
		String wsCity = city.toLowerCase();

		URL url = new URL("http://localhost:" + wsPort + "/" + wsCity + "?wsdl");
		QName qName = new QName("http://serverside.carlo.replicas/", "ServerOperationsImplService");

		Service service = Service.create(url, qName);
		ServerOperationsInterface server = service.getPort(ServerOperationsInterface.class);

		return server;
	}

	public static void sendPacket(InetAddress address, int port, String msg) {
		int fePort = 0;
		DatagramSocket aSocket = null;

		System.out.println("Sending to " + address + ": " + port);
		try {
			aSocket = new DatagramSocket();
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

	private static String getWSPort(String city) {
		String port = "";
		if (city.equals("MTL"))
			port = Integer.toString(mtlWServerPort);
		else if (city.equals("TOR"))
			port = Integer.toString(torWServerPort);
		else if (city.equals("VAN"))
			port = Integer.toString(vanWServerPort);
		else
			port = "9999";
		return port;
	}

	private static String getCity(String data) {
		String city = data.substring(0, 3);
		return city;
	}

	private static ServerOperationsImpl createDB(String city) throws Exception {
		String fest1ID = city + "M010122";
		Event fest1 = new Event(10);
		String fest2ID = city + "M020122";
		Event fest2 = new Event(20);
		String fest3ID = city + "A030122";
		Event fest3 = new Event(30);
		String fest4ID = city + "E040122";
		Event fest4 = new Event(40);
		String fest5ID = city + "E050122";
		Event fest5 = new Event(50);
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
		Event fest6 = new Event(10);
		String fest7ID = city + "M070122";
		Event fest7 = new Event(20);
		String fest8ID = city + "A080122";
		Event fest8 = new Event(30);
		HashMap<String, Event> sampleF2 = new HashMap<String, Event>() {
			{
				put(fest6ID, fest6);
				put(fest7ID, fest7);
				put(fest8ID, fest8);
			}
		};

		String fest9ID = city + "M090122";
		Event fest9 = new Event(10);
		String fest10ID = city + "M100122";
		Event fest10 = new Event(20);
		String fest11ID = city + "A110122";
		Event fest11 = new Event(30);
		HashMap<String, Event> sampleF3 = new HashMap<String, Event>() {
			{
				put(fest9ID, fest9);
				put(fest10ID, fest10);
				put(fest11ID, fest11);
			}
		};

		HashMap<String, HashMap<String, Event>> sampleDB = new HashMap<String, HashMap<String, Event>>() {
			{
				put("Art Gallery", sampleF1);
				put("Concerts", sampleF2);
				put("Theatre", sampleF3);
			}
		};

		ServerOperationsImpl serverOps = new ServerOperationsImpl(city, sampleDB);
		return serverOps;
	}

	public static String getEventType(String eventType) {
		if (eventType.equals("art"))
			return "Art Gallery";
		else if (eventType.equals("concert"))
			return "Concerts";
		else if (eventType.equals("theatre"))
			return "Theatre";
		return "Error";
	}
}
