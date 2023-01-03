package replicas.philippe.Server;

import replicas.philippe.model.Response;
import replicas.philippe.model.Event;
import replicas.philippe.model.EventType;

import javax.xml.ws.Endpoint;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.*;

public class FestivalServer extends Thread {

	private ParticipantImpl participant;
	private AdminImpl admin;
	private String city;
	Map<String, Map<String, Event>> eventRecords;
	Map<String, Integer> socketPorts = new HashMap<String, Integer>() {
		{
			put("MTL", 10110);
			put("TOR", 10111);
			put("VAN", 10112);
		}
	};
	Map<String, Integer> ports = new HashMap<String, Integer>() {
		{
			put("MTL", 16080);
			put("TOR", 16089);
			put("VAN", 16099);
		}
	};

	public FestivalServer(String city, Map<String, Map<String, Event>> eventRecords) {
//      String city = args[0];
		System.out.println(city);
//      populateEventRecords(city);
		this.eventRecords = eventRecords;
		this.city = city;

		ParticipantImpl participant = new ParticipantImpl();
		participant.setRegion(city);
		participant.setSocketPorts(socketPorts);
		participant.setEventRecords(eventRecords);
		participant.setLogger(String.format("ServerLogs-%s.log", city));

		String url = String.format("http://localhost:%d/participant-%s", ports.get(city), city);
		System.out.println(url);
		Endpoint endpoint = Endpoint.publish(url, participant);
		System.out.println("ParticipantServer ready and waiting..." + endpoint.isPublished());

		AdminImpl admin = new AdminImpl();
		admin.setRegion(city);
		admin.setSocketPorts(socketPorts);
		admin.setEventRecords(eventRecords);
		admin.setLogger(String.format("ServerLogs-%s.log", city));

		url = String.format("http://localhost:%d/admin-%s", ports.get(city), city);
		System.out.println(url);
		endpoint = Endpoint.publish(url, admin);
		System.out.println("AdminServer ready and waiting..." + endpoint.isPublished());
	}

	public void run() {
		DatagramSocket udpSocket = null;
		try {
			udpSocket = new DatagramSocket(socketPorts.get(city));
		} catch (SocketException e) {
			e.printStackTrace();
		}
		byte[] buffer = new byte[100];
		byte[] udpByteReply;
		DatagramPacket reply;

		while (true) {
			System.out.println("UDP Server is running");
			try {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				udpSocket.receive(request);
				String message = new String(Arrays.copyOfRange(request.getData(), request.getOffset(),
						request.getOffset() + request.getLength())).trim() + "";
				System.out.println(message);
				String[] messageContent = message.split("-");
				System.out.println(messageContent[0]);
				System.out.println(messageContent[1]);
				String replyMessage = "";

				ParticipantImpl tempParticipant;
				String[] parameters;
				Response response;
				switch (messageContent[0]) {
				case "schedule":
					tempParticipant = new ParticipantImpl();
					tempParticipant.setEventRecords(eventRecords);
					replyMessage = tempParticipant.getParticipantEvents(messageContent[1]);
					break;
				case "list":
					AdminImpl tempAdmin = new AdminImpl();
					tempAdmin.setEventRecords(eventRecords);
					replyMessage = tempAdmin.getAvailableEventSlot(messageContent[1]);
					System.out.println(replyMessage);
					break;
				case "available":
					tempParticipant = new ParticipantImpl();
					tempParticipant.setEventRecords(eventRecords);
					parameters = messageContent[1].split(",");
					replyMessage = tempParticipant.isEventAvailable(parameters[0], parameters[1]);
					break;
				case "reserve":
					tempParticipant = new ParticipantImpl();
					tempParticipant.setEventRecords(eventRecords);
					tempParticipant.setSocketPorts(socketPorts);
					tempParticipant.setRegion(city);
					tempParticipant.setLogger(String.format("ServerLogs-%s.log", city));
					parameters = messageContent[1].split(",");
					response = tempParticipant.reserveTicket(parameters[0], parameters[2], parameters[1]);
					replyMessage = Boolean.toString(response.isSuccessful());
					break;
				case "cancel":
					tempParticipant = new ParticipantImpl();
					tempParticipant.setEventRecords(eventRecords);
					tempParticipant.setLogger(String.format("ServerLogs-%s.log", city));
					parameters = messageContent[1].split(",");
					response = tempParticipant.cancelTicket(parameters[0], parameters[1]);
					replyMessage = Boolean.toString(response.isSuccessful());
					break;
				}

				udpByteReply = replyMessage.getBytes();
				reply = new DatagramPacket(udpByteReply, udpByteReply.length, request.getAddress(), request.getPort());
				udpSocket.send(reply);
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}
	}

	private void populateEventRecords(String region) {
		Event MTLArtEvent1 = new Event(5, EventType.ART_GALLERY.value); // MTLA201022
		Event MTLArtEvent2 = new Event(5, EventType.ART_GALLERY.value); // MTLA311022
		MTLArtEvent2.addParticipant("MTLP1234");
		Event MTLArtEvent3 = new Event(0, EventType.ART_GALLERY.value); // MTLA051122
		Event MTLArtEvent4 = new Event(5, EventType.ART_GALLERY.value); // MTLE311022
		Event MTLArtEvent5 = new Event(5, EventType.ART_GALLERY.value); // MTLE011122
		Event MTLArtEvent6 = new Event(5, EventType.ART_GALLERY.value); // MTLA011222
		MTLArtEvent6.addParticipant("MTLP1234");
		Event MTLConcertEvent1 = new Event(5, EventType.CONCERT.value); // MTLA311222
		Event TORConcertEvent1 = new Event(4, EventType.CONCERT.value); // TORM021122
		TORConcertEvent1.addParticipant("MTLP1234");
		Event TORConcertEvent2 = new Event(4, EventType.CONCERT.value); // TORE010123
		Event VANTheatreEvent1 = new Event(4, EventType.THEATRE.value); // VANE041122
		VANTheatreEvent1.addParticipant("MTLP1234");
		Event TORTheatreEvent1 = new Event(4, EventType.THEATRE.value); // TORA150123
		Event TORTheatreEvent2 = new Event(5, EventType.THEATRE.value); // TORE011122
		Event MTLTheatreEvent1 = new Event(4, EventType.THEATRE.value); // MTLE150123

		switch (region) {
		case "MTL":
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLA201022", MTLArtEvent1);
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLA311022", MTLArtEvent2);
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLA051122", MTLArtEvent3);
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLE311022", MTLArtEvent4);
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLE011122", MTLArtEvent5);
			eventRecords.get(EventType.ART_GALLERY.value).put("MTLA011222", MTLArtEvent6);
			eventRecords.get(EventType.CONCERT.value).put("MTLA311222", MTLConcertEvent1);
			eventRecords.get(EventType.THEATRE.value).put("MTLE150123", MTLTheatreEvent1);
			break;
		case "TOR":
			eventRecords.get(EventType.CONCERT.value).put("TORM021122", TORConcertEvent1);
			eventRecords.get(EventType.CONCERT.value).put("TORE010123", TORConcertEvent2);
			eventRecords.get(EventType.THEATRE.value).put("TORA150123", TORTheatreEvent1);
			eventRecords.get(EventType.THEATRE.value).put("TORE011122", TORTheatreEvent2);
			break;
		case "VAN":
			eventRecords.get(EventType.THEATRE.value).put("VANE041122", VANTheatreEvent1);
			break;
		}
	}
}
