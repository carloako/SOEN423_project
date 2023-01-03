package dtrs;

//import java.rmi.RemoteException;
//import java.rmi.registry.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.RemoteException;

import javax.xml.ws.Endpoint;

import dtrs.com.web.service.DTRSWebServicesImpl;
import dtrs.com.web.service.DTRSWebServices;
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
				this.serverendpoint="http://localhost:8081/montreal";
				break;
			case "TOR":
				this.serverName=DTRSWebServicesImpl.Server_Toronto;
				this.serverUdpPort=DTRSWebServicesImpl.Toronto_server_udp_port;
				this.serverendpoint="http://localhost:8081/toronto";
				break;
				
			case "VAN":
				this.serverName=DTRSWebServicesImpl.Server_Vancouver;
				this.serverUdpPort=DTRSWebServicesImpl.Vancouver_server_udp_port;
				this.serverendpoint="http://localhost:8081/vancouver";
				break;
		}
		try {
			DTRSWebServicesImpl webservice= new DTRSWebServicesImpl(serverID,serverName);
			Endpoint endpoint = Endpoint.publish(serverendpoint, webservice);
			
			
			System.out.println(endpoint.isPublished());
            System.out.println(serverName + " Server is Up & Running");
            Logger.ServerLog(serverID, " Server is Up & Running");
            addTestData(webservice);
            
            
            
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
					String result=obj.reserveTicket(clientID,eventId,eventType);
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
	
	
	

}
