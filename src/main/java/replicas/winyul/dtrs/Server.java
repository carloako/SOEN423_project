package replicas.winyul.dtrs;

//import java.rmi.RemoteException;
//import java.rmi.registry.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.ws.Endpoint;

import replicas.winyul.dtrs.com.web.service.DTRSWebServicesImpl;
import replicas.winyul.dtrs.com.web.service.DTRSWebServices;
/*
//import org.omg.CORBA.ORB;
//import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import DTRS.CORBAinterface;
import DTRS.CORBAinterfaceHelper;
*/
@SuppressWarnings("unused")
public class Server {
	private String serverID;
	private String serverName;
	private int serverUdpPort;
	private String serverendpoint;



	public Server(String serverId,String[] args) throws Exception{
		this.serverID=serverId;
		switch(serverID) {
			case"MTL":
				this.serverName=DTRSWebServicesImpl.Server_Montreal;
				this.serverUdpPort=DTRSWebServicesImpl.Montreal_server_udp_port;
				this.serverendpoint="http://localhost:12081/montreal";
				break;
			case "TOR":
				this.serverName=DTRSWebServicesImpl.Server_Toronto;
				this.serverUdpPort=DTRSWebServicesImpl.Toronto_server_udp_port;
				this.serverendpoint="http://localhost:12081/toronto";
				break;

			case "VAN":
				this.serverName=DTRSWebServicesImpl.Server_Vancouver;
				this.serverUdpPort=DTRSWebServicesImpl.Vancouver_server_udp_port;
				this.serverendpoint="http://localhost:12081/vancouver";
				break;
		}
		try {
			DTRSWebServicesImpl webservice= new DTRSWebServicesImpl(serverID,serverName);
			populateDB(serverID, webservice.getAllEvents());
			Endpoint endpoint = Endpoint.publish(serverendpoint, webservice);


			System.out.println(endpoint.isPublished());
            System.out.println(serverName + " Server is Up & Running");
            Logger.ServerLog(serverID, " Server is Up & Running");
//            addTestData(webservice);



            /*
			//create the server registery object to register itself on the unique port
			Registry registry= LocateRegistry.createRegistry(serverPort);

			//bind the server obj to the registery with unique name and implementation
			registry.bind(Client.SERVER_BIND, Obj);
			System.out.println(serverName+  " Server is up and running");
			Logger.ServerLog(serverID, " Server is up and Running");
			addTestData(Obj);
			*/
			Runnable task= () ->{
			listenForRequest(webservice,serverUdpPort,serverName,serverID);
			};
			Thread thread= new Thread(task);
			thread.start();

			}
		catch(Exception e) {
			e.printStackTrace();
			Logger.ServerLog(serverID, "Exception: " + e);
		}
		  System.out.println(serverName + " Server Shutting down");
	      Logger.ServerLog(serverID, " Server Shutting down");
	}

	private void addTestData(DTRSWebServicesImpl obj) throws RemoteException {
		switch(serverID) {
			case "MTL":
				//id ,type capacity
				obj.addReservationSlot("MTLA101122",Event.ART_GALLERY,4);
				obj.addReservationSlot("MTLE100822", Event.CONCERT,5 );
				obj.addReservationSlot("MTLM101222",Event.THEATER , 7);
				break;
			case "TOR":
				obj.addReservationSlot("TORE101122", Event.CONCERT,5 );
				obj.addReservationSlot("TORE111122", Event.THEATER,8 );
				break;
			case "VAN":
				obj.addReservationSlot("VANM101122",Event.ART_GALLERY,1 );
				obj.addReservationSlot("VANM101122",Event.CONCERT ,2 );
				obj.addReservationSlot("VANM101122",Event.THEATER , 7);
				break;
		}
	}

	private static void listenForRequest(DTRSWebServicesImpl obj, int serverUdpPort,String serverName,String serverID) {
		DatagramSocket socket=null;
		String results="";
		try {
			socket=new DatagramSocket(serverUdpPort);
			byte[] buffer=new byte[1000];
			System.out.println(serverName+ " UDP Server Started at port "+ socket.getLocalPort()+"......");
			Logger.ServerLog(serverID, "UDP Server Started at port " + socket.getLocalPort());

			while(true) {
				DatagramPacket request= new DatagramPacket(buffer,buffer.length);
				socket.receive(request);
				String sentence=new String(request.getData(),0,request.getLength());
				String[] parts= sentence.split(";");
				String method=parts[0];
				String clientID=parts[1];
				String eventType=parts[2];
				String eventId=parts[3];
				if(method.equalsIgnoreCase("remoteEvent")) {
					Logger.ServerLog(serverID, clientID,"UDP request received " + method+ " ", "eventID: "+ eventId + "eventType: "+ eventType+ " ","..." );
					String result=obj.removeEventUDP(clientID,eventId,eventType);
					results=result+ ";";
				}
				else if(method.equalsIgnoreCase("listReservationSlotAvailable")) {
					Logger.ServerLog(serverID, clientID,"UDP request received " + method+ " ", "eventID: "+ eventId + "eventType: "+ eventType+ " ","..." );
					String result=obj.listEventAvailabilityUDP(eventType);
					results=result+ ";";
				}
				else if(method.equalsIgnoreCase("bookEvent")) {
					Logger.ServerLog(serverID, clientID,"UDP request received " + method+ " ", "eventID: "+ eventId + "eventType: "+ eventType+ " ","..." );
					System.out.println("HERE RESERVING TICKET");
					String result=obj.reserveTicket(clientID,eventId,eventType);
					System.out.println("Reservation done");
					results=result+ ";";
				}
				else if(method.equalsIgnoreCase("cancelEvent")) {
					Logger.ServerLog(serverID, clientID,"UDP request received " + method+ " ", "eventID: "+ eventId + "eventType: "+ eventType+ " ","..." );
					String result=obj.cancelTicket(clientID,eventId);
					results=result+ ";";
				}
				byte[]sendData=results.getBytes();
				DatagramPacket reply= new DatagramPacket(sendData,results.length(),request.getAddress(),request.getPort());
				socket.send(reply);
				Logger.ServerLog(serverID, clientID," UDP reply sent " + method+ " ", "eventID:" + eventId+ " eventType" + eventType+ " ", results);
			}
		}
		catch(SocketException e) {
			System.out.println("SocketException "+ e.getMessage());
		}
		catch(IOException e) {
			System.out.println("IOException"+ e.getMessage());
		}
		finally {
			if(socket!=null)
				socket.close();
		}
	}

	public static void populateDB(String city, Map<String,Map<String,Event>> events) {
		Event temp[] = new Event[5];
		String fest1ID = city + "M010122";
		temp[0] = new Event(Event.ART_GALLERY, fest1ID, 10);
		String fest2ID = city + "M020122";
		temp[1] = new Event(Event.ART_GALLERY, fest2ID, 20);
		String fest3ID = city + "A030122";
		temp[2] = new Event(Event.ART_GALLERY, fest3ID, 30);
		String fest4ID = city + "E040122";
		temp[3] = new Event(Event.ART_GALLERY, fest4ID, 40);
		String fest5ID = city + "E050122";
		temp[4] = new Event(Event.ART_GALLERY, fest5ID, 50);
		for (int i = 0; i < temp.length; i++)
			events.get(Event.ART_GALLERY).put(temp[i].getEventID(), temp[i]);

		temp = new Event[3];
		String fest6ID = city + "M060122";
		temp[0] = new Event(Event.CONCERT, fest6ID, 10);
		String fest7ID = city + "M070122";
		temp[1] = new Event(Event.CONCERT, fest7ID, 20);
		String fest8ID = city + "A080122";
		temp[2] = new Event(Event.CONCERT, fest8ID, 30);
		for (int i = 0; i < temp.length; i++)
			events.get(Event.CONCERT).put(temp[i].getEventID(), temp[i]);

		temp = new Event[3];
		String fest9ID = city + "M090122";
		temp[0] = new Event(Event.THEATER, fest9ID, 10);
		String fest10ID = city + "M100122";
		temp[1] = new Event(Event.THEATER, fest10ID, 20);
		String fest11ID = city + "A110122";
		temp[2] = new Event(Event.THEATER, fest11ID, 30);
		for (int i = 0; i < temp.length; i++)
			events.get(Event.THEATER).put(temp[i].getEventID(), temp[i]);
	}
}
