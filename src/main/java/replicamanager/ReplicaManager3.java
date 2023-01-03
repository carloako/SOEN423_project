package replicamanager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.Arrays;
import java.nio.charset.MalformedInputException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import replicas.winyul.dtrs.Server;
import replicas.winyul.dtrs.com.web.service.DTRSWebServices;

public class ReplicaManager3 extends Thread {

	static int rmPort = Address.RM3_PORT;

	private static int bufferIndex = 1;
	private static TreeMap<Integer, Request> requestBuffer = new TreeMap<Integer, Request>();

	public static DTRSWebServices Obj;

	public static void main(String[] args) throws Exception {

		InetAddress sqAddress = InetAddress.getByName(Address.SQ_ADDRESS);
		int sqPort = Address.SQ_PORT;

		InetAddress feAddress = InetAddress.getByName(Address.FE_ADDRESS);
		int fePort = Address.FE_PORT;

		String mtl = "MTL";
		Runnable task1 = () -> {
			try {
				new Server("MTL", args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		Runnable task2 = () -> {
			try {
				new Server("TOR", args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		Runnable task3 = () -> {
			try {
				new Server("VAN", args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		Thread thread1 = new Thread(task1);
		thread1.start();
		Thread thread2 = new Thread(task2);
		thread2.start();
		Thread thread3 = new Thread(task3);
		thread3.start();

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
					requestBuffer.put(Integer.parseInt(sqNum), newReq);

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
							thread1 = new Thread(task1);
							thread1.start();
						} else if (city.equals("TOR")) {
							thread2 = new Thread(task2);
							thread2.start();
						} else if (city.equals("VAN")) {
							thread3 = new Thread(task3);
							thread3.start();
						} else {
							System.out.println("Server reset error");
						}
						// alert other rms
//						sendPacket(rm1Add, port1, "Success");
//						sendPacket(rm2Add, port2, "Success");
						// reply to fe
//						sendPacket(dgram.getAddress(), dgram.getPort(), "Success");
					} else {
						System.out.println("Waiting for replica restart");
						buffer = new byte[1000];
						DatagramPacket checkServerDgram = new DatagramPacket(buffer, buffer.length);
						aSocket.receive(checkServerDgram);
						String recvdata = new String(checkServerDgram.getData());
						System.out.println(recvdata);
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

			String defaultCity = "MTL";
			String userID = null;
			String eventType = null;
			String eventID = null;
			String serverResponse = null;
			String returnVal = null;
			System.out.println("Starting process");
			try {
				DTRSWebServices server = null;
//				switch (method) {
				switch (methodString) {
//				case 1:
				case "addReservationSlot":
					System.out.println("Adding reservation slot:");
					eventID = params[0];
					eventType = getEventType(params[1]);
					int capacity = Integer.parseInt(params[2]);

					System.out.println(eventType + ", " + eventID + ", " + capacity);
					System.out.println(getCity(eventID));

					if(getServerID(getCity(eventID)).equals("1"))
						throw new Exception();

					serverResponse = Obj.addReservationSlot(eventID, eventType, capacity);
					if (serverResponse.substring(0, "Success".length()).equals("Success")) {
						returnVal = "1";
					} else
						returnVal = "0";
					break;
//				case 2:
				case "removeReservationSlot":
					System.out.println("Removing reservation slot:");
					eventID = params[0];
					eventType = getEventType(params[1]);

//					server = getServer(getCity(eventID));
					getServerID(eventID);

					serverResponse = Obj.removeReservationSlot(eventID, eventType);
					if (serverResponse.substring(0, "Success".length()).equals("Success")) {
						returnVal = "1";
					} else
						returnVal = "0";
					break;
//				case 3:
				case "listReservationSlotAvailable":
					System.out.println("Listing available reservation slot: ");
					eventType = getEventType(params[0]);

//					server = getServer(defaultCity);
					getServerID(defaultCity + "M");

					serverResponse = Obj.listReservationSlotAvailable(eventType);
					if (!serverResponse.trim().isEmpty()) {
						returnVal = "1:";
						String temp = serverResponse.substring(serverResponse.indexOf(':'));
						String fullTemp[] = temp.split(",");
						for (int i = 0; i < fullTemp.length; i++) {
							String j = fullTemp[i];
							int cIndex = j.indexOf("Capacity: ");
							String c = j.substring(cIndex + "Capacity: ".length());
							int index = j.indexOf("MTL") != -1 ? j.indexOf("MTL") : j.indexOf("TOR") != -1 ? j.indexOf("TOR") : j.indexOf("VAN") != -1 ? j.indexOf("VAN") : -1;
							fullTemp[i] = j.substring(index, index + 10) + " " + c.substring(0, c.indexOf("["));
//							if (i != fullTemp.length - 1)
//								returnVal += ",";
						}
						Arrays.sort(fullTemp);
						for (int i = 0; i < fullTemp.length; i++) {
							returnVal += fullTemp[i];
							if (i != fullTemp.length - 1)
								returnVal += ",";
						}
					} else
						returnVal = "0";
					System.out.println(returnVal);
					break;
//				case 4:
				case "reserveTicket":
					System.out.println("Reserving ticket: ");
					eventType = getEventType(params[2]);
					eventID = params[1];
					userID = params[0];

//					server = getServer(getCity(eventID));
					System.out.println("HERE");
					getServerID(userID);

					serverResponse = Obj.reserveTicket(userID, eventID, eventType);
					System.out.println(serverResponse);
					if (serverResponse.substring(0, "Success".length()).equals("Success")) {
						returnVal = "1";
					} else
						returnVal = "0";
					break;
//				case 5:
				case "getEventSchedule":
					System.out.println("Getting event schedule: ");
					userID = params[0]; // TODO need user ID here

//					server = getServer(getCity(userID));
					getServerID(userID);

					serverResponse = Obj.getEventSchedule(userID);
					if (!serverResponse.trim().isEmpty()) {
						String temp = serverResponse.substring(0, serverResponse.length() - 1);
						String events[] = temp.split(",");
						Arrays.sort(events);
						returnVal = "1:";
						for (int i = 0; i < events.length; i++) {
							returnVal += events[i];
							if (i != events.length -1)
								returnVal += ",";
						}
					} else returnVal = "0";
					break;
//				case 6:
				case "cancelTicket":
					System.out.println("Cancelling ticket: ");
					userID = params[0];
					eventID = params[1];

//					server = getServer(getCity(eventID));
					getServerID(userID);

					serverResponse = Obj.cancelTicket(userID, eventID);
					if (serverResponse.substring(0, "Success".length()).equals("Success")) {
						returnVal = "1";
					} else
						returnVal = "0";
					break;
//				case 7:
				case "exchangeTickets":
					System.out.println("Exchanging ticket: ");
					System.out.println("params[3] = " + params[3] + "|");
					userID = params[0];
					String tbcEventID = params[1];
					String tbaEventID = params[2];
					eventType = getEventType(params[3]);

					System.out.println(eventType);

//					server = getServer(getCity(tbcEventID));
					getServerID(userID);

					serverResponse = Obj.exchangeTickets(userID, tbcEventID, tbaEventID, eventType);
					if (serverResponse.substring(0, "Success".length()).equals("Success")) {
						returnVal = "1";
					} else
						returnVal = "0";
					break;
				}
				bufferIndex++;
//				sendPacket(dgram.getAddress(), dgram.getPort(), returnVal);
				sendPacket(InetAddress.getByName(Address.FE_ADDRESS), 8999, returnVal == null ? "" : returnVal);
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

	private static String getCity(String data) {
		String city = data.substring(0, 3);
		return city;
	}

	public static String getServerID(String userID) throws MalformedURLException {
		String branch = userID.substring(0, 3);
		System.out.println(branch);
		if (branch.equalsIgnoreCase("MTL")) {
			URL montrealurl = new URL("http://localhost:12081/montreal?wsdl");
			QName montrealqname = new QName("http://service.web.com.dtrs.winyul.replicas/",
					"DTRSWebServicesImplService");
			Service montrealService = Service.create(montrealurl, montrealqname);
			Obj = montrealService.getPort(DTRSWebServices.class);
			return branch;
		} else if (branch.equalsIgnoreCase("TOR")) {
			URL torontourl = new URL("http://localhost:12081/toronto?wsdl");
			QName torontoqname = new QName("http://service.web.com.dtrs.winyul.replicas/",
					"DTRSWebServicesImplService");
			Service torontoService = Service.create(torontourl, torontoqname);
			Obj = torontoService.getPort(DTRSWebServices.class);
			return branch;
		} else if (branch.equalsIgnoreCase("VAN")) {
			URL vancouverurl = new URL("http://localhost:12081/vancouver?wsdl");
			QName vancouverqname = new QName("http://service.web.com.dtrs.winyul.replicas/",
					"DTRSWebServicesImplService");
			Service vancouverService = Service.create(vancouverurl, vancouverqname);
			Obj = vancouverService.getPort(DTRSWebServices.class);
			return branch;
		}
		return "1";
	}

	public static String getEventType(String eventType) {
		if (eventType.equals("art"))
			return "Art gallery";
		else if (eventType.equals("concert"))
			return "Concert";
		else if (eventType.equals("theatre"))
			return "Theater";
		return "Error";
	}
}
