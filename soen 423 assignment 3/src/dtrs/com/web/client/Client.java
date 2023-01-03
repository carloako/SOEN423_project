package dtrs.com.web.client;
import java.net.URL;
//import java.rmi.RemoteException;
//import java.rmi.registry.*;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import dtrs.Event;
import dtrs.Logger;
import dtrs.com.web.service.DTRSWebServices;

//import org.omg.CORBA.ORB;
//import org.omg.CosNaming.*;



public class Client{
	public static final int ADMIN_TYPE=1;
	public static final int PARTICIPANT_TYPE=2;
	//client navigation option
	public static final int PARTICIPANT_BOOK_EVENT=1;
	public static final int PARTICIPANT_GET_BOOKING=2;
	public static final int PARTICIPANT_CANCEL_BOOKING=3;
	public static final int PARTICIPANT_EXCHANGE_TICKET=4;
	public static final int PARTICIPANT_LOGOUT=5;
	public static final int ADMIN_ADD_EVENT=1;
	public static final int ADMIN_REMOVE_EVENT=2;
	public static final int ADMIN_LIST_EVENT_AVAILABILITY=3;
	public static final int ADMIN_BOOK_EVENT=4;
	public static final int ADMIN_GET_BOOKING=5;
	public static final int ADMIN_CANCEL_BOOKING=6;
	public static final int ADMIN_EXCHANGE_TICKET=7;
	public static final int ADMIN_LOGOUT=8;	
	
	private static DTRSWebServices Obj;
	public static Service montrealService;
    public static Service torontoService;
    public static Service vancouverService;
	//server port location
	

	
	//Scanner
	static Scanner input;
	
	
	public static void main(String args[]) throws Exception {
		try {
			/*
			ORB orb=ORB.init(args,null);
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			NamingContextExt ncRef= NamingContextExtHelper.narrow(objRef);
			*/
			URL montrealurl= new URL("http://localhost:8081/montreal?wsdl");
			QName montrealqname=new QName("http://service.web.com.dtrs/","DTRSWebServicesImplService");
			montrealService=Service.create(montrealurl,montrealqname);
			
			URL torontourl= new URL("http://localhost:8081/montreal?wsdl");
			QName torontoqname=new QName("http://service.web.com.dtrs/","DTRSWebServicesImplService");
			torontoService=Service.create(torontourl,torontoqname);
			
			URL vancouverurl= new URL("http://localhost:8081/montreal?wsdl");
			QName vancouverqname=new QName("http://service.web.com.dtrs/","DTRSWebServicesImplService");
			vancouverService=Service.create(vancouverurl,vancouverqname);
			
			start();
			
			
		
		}catch(Exception e) {
			System.out.println("Client ORB Initialization exception "+ e);
			e.printStackTrace();
		}
	}	
	private static void start() throws Exception{
		input=new Scanner(System.in);
		System.out.println("Please enter your Client ID");
		String clientId= input.next().trim().toUpperCase();
		Logger.ClientLog(clientId, "login attempted");
		if(clientId.equalsIgnoreCase("contest")) {
			contest();
		}
		else {	
			switch(checkUserType(clientId)) {
			case ADMIN_TYPE:
				try {
					System.out.println("Admin Login successful (" + clientId + ")");
					Logger.ClientLog(clientId, " admin Login successful");
					Admin(clientId);
				}
				catch (Exception e) {
	                e.printStackTrace();
	            }
				break;
			case PARTICIPANT_TYPE:
				try {
					System.out.println("Participant Login successful (" + clientId + ")");
					Logger.ClientLog(clientId, " Participant Login successful");
					Participant(clientId);
				}
				catch (Exception e) {
	                e.printStackTrace();
	            }
				break;
			default:
	            System.out.println("!!UserID is not in correct format");
	            Logger.ClientLog(clientId, " UserID is not in correct format");
	            start();
			}
		}		
		
	}
	private static void contest() throws Exception {
		 System.out.println("Concurrency Test Starting for Reserve Ticket");
	        System.out.println("Connecting Montreal Server...");
	        String eventType =Event.CONCERT ;
	        String eventID = "MTLE101020";
	        DTRSWebServices servant= montrealService.getPort(DTRSWebServices.class);
	       
	        System.out.println("adding " + eventID + " " + eventType + " with capacity 2 to Montreal Server...");
	        String response = servant.addReservationSlot(eventID, eventType, 2);
	        System.out.println(response);
	        
	        Runnable task1 = () -> {
	            String customerID = "MTLP1111";
	            System.out.println("Connecting Montreal Server for " + customerID);
	            String res = servant.reserveTicket(customerID, eventID, eventType);
	            System.out.println("Booking response for " + customerID + " " + res);
	        };
	        Runnable task2 = () -> {
	            String customerID = "MTLP2222";
	            System.out.println("Connecting Montreal Server for " + customerID);
	            String res = servant.reserveTicket(customerID, eventID, eventType);
	            System.out.println("Booking response for " + customerID + " " + res);
	        };
	        Runnable task3 = () -> {
	            String customerID = "MTLP3333";
	            System.out.println("Connecting Montreal Server for " + customerID);
	            String res = servant.reserveTicket(customerID, eventID, eventType);
	            System.out.println("Booking response for " + customerID + " " + res);
	        };
	        Runnable task4 = () -> {
	    
	            String res = servant.cancelTicket("MTLP1111", eventID);
	            System.out.println("Canceling response for MTLP1111" + " " + res);

	            res = servant.cancelTicket("MTLP2222", eventID);
	            System.out.println("Canceling response for MTLP2222" + " " + res);

	            res = servant.cancelTicket("MTLP3333", eventID);
	            System.out.println("Canceling response for MTLP3333" + " " + res);
	        };

	        Runnable task5 = () -> {
	           
	            String res = servant.removeReservationSlot(eventID, eventType);
	            System.out.println("removeEvent response for " + eventID + " " + res);
	        };
	        Thread thread1 = new Thread(task1);
	        Thread thread2 = new Thread(task2);
	        Thread thread3 = new Thread(task3);
	        Thread thread4 = new Thread(task4);
	        Thread thread5 = new Thread(task5);

	        thread1.start();
	        thread2.start();
	        thread3.start();

	        thread1.join();
	        thread2.join();
	        thread3.join();

	        //cancelling the event for clients
	        thread4.start();
	        thread4.join();
//	        if (!thread1.isAlive() && !thread2.isAlive() && !thread3.isAlive() && !thread4.isAlive() && !thread5.isAlive()) {
	        System.out.println("Concurrency Test Finished for BookEvent");
	        thread5.start();
	        thread5.join();
	        start();
	

	        
	        
	        
	        
	        
	}
	private static void Participant(String clientId) throws Exception{
		String serverID=getServerID(clientId);
		if(serverID.equals("1")) {
			return;
		}
		//Registry registry= LocateRegistry.getRegistry(serverPort);
		//RMIinterface Obj=(RMIinterface) registry.lookup(SERVER_BIND);
		
		//DTRSWebServices Obj= Service.getPort(DTRSWebServices.class); 
		
		
		
		boolean logOn=true;
	    printMenu(PARTICIPANT_TYPE);
		int menuSelection=input.nextInt();
		String eventType;
		String eventID;
		String serverResponse;
		
		switch(menuSelection) {
			case PARTICIPANT_BOOK_EVENT:
				eventType=promptForEventType();
				eventID=promptForEventId();
				Logger.ClientLog(clientId, " attempting to book an event");
				serverResponse=Obj.reserveTicket(clientId, eventID, eventType);
				System.out.println(serverResponse);
				Logger.ClientLog(clientId,"BookEvent", ("EventType:" + eventType +" EventID: " +eventID +" "), serverResponse);
				break;
			case PARTICIPANT_GET_BOOKING:
				Logger.ClientLog(clientId, " attempting to get Booking Schedule");
				serverResponse=Obj.getEventSchedule(clientId);
				System.out.println(serverResponse);
				Logger.ClientLog(clientId,"View Booking","null", serverResponse);
				break;
			case PARTICIPANT_CANCEL_BOOKING:
				eventType=promptForEventType();
				eventID=promptForEventId();
				Logger.ClientLog(clientId, " attempting to cancel an event");
				serverResponse=Obj.cancelTicket(clientId, eventID);
				System.out.println(serverResponse);
				Logger.ClientLog(clientId,"CancelEvent", ("EventType:" + eventType +" EventID: " +eventID +" "), serverResponse);
				break;
			case PARTICIPANT_EXCHANGE_TICKET:
				System.out.println("Please enter the old event you attempt to replace");
				eventType=promptForEventType();
				eventID=promptForEventId();
				System.out.println("Please enter the new event that you attempt to acquire");
				String newEventType=promptForEventType();
				String newEventID=promptForEventId();
				Logger.ClientLog(clientId, "attempting to exchange tickets");
				serverResponse=Obj.exchangeTickets(clientId, eventID, newEventID,newEventType);
				System.out.println(serverResponse);
				Logger.ClientLog(clientId, "exchange ticket "," oldEventID "+ eventID+" oldEventType "+ eventType+ " newEventID "+ newEventID+ " newEventType "+ newEventType, serverResponse);
				break;
			
			case PARTICIPANT_LOGOUT:
				logOn=false;
				Logger.ClientLog(clientId, " attempting to log out");
				start();
				break;
		}
		if(logOn) {
			Participant(clientId);
			
		}
	}
	private static void Admin(String adminId) throws Exception{
		String serverID=getServerID(adminId);
		if(serverID.equals("1")) {
			return;
		}
		//Registry registry= LocateRegistry.getRegistry(serverPort);
		//RMIinterface Obj=(RMIinterface)registry.lookup(SERVER_BIND);
		
		
		boolean logOn=true;
		printMenu(ADMIN_TYPE);
		String clientId;
		String eventType;
		String eventID;
		String serverResponse;
		
		int capacity;
		int menuSelection=input.nextInt();
		switch(menuSelection) {
		case ADMIN_ADD_EVENT:
			eventType=promptForEventType();
			eventID=promptForEventId();
			capacity=promptForCapacity();
			Logger.ClientLog(adminId, " attempting to add an event");
			serverResponse=Obj.addReservationSlot(eventID, eventType,capacity);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId," Event ADD ", ("EventType:" + eventType +" EventID: " +eventID +" Capacity: " + capacity + " "), serverResponse);
			break;
		case ADMIN_REMOVE_EVENT:
			eventType=promptForEventType();
			eventID=promptForEventId();
			Logger.ClientLog(adminId, " attempting to remode an event");
			serverResponse=Obj.removeReservationSlot(eventID, eventType);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId," remove event ", (" EventType:" + eventType +" EventID: " +eventID +" "), serverResponse);
			break;
		case ADMIN_LIST_EVENT_AVAILABILITY:
			eventType=promptForEventType();
			Logger.ClientLog(adminId, " attempting to List Event Availability");
			serverResponse=Obj.listReservationSlotAvailable(eventType);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId," remove event ", (" EventType:" + eventType +" "), serverResponse);
			break;
			
		case ADMIN_BOOK_EVENT:
			clientId=promptForClientId(adminId.substring(0,3));
			eventType=promptForEventType();
			eventID=promptForEventId();
			Logger.ClientLog(adminId, " attempting to book an event");
			serverResponse=Obj.reserveTicket(clientId, eventID, eventType);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId,"BookEvent", (" Client ID: " + clientId + " EventType:" + eventType +" EventID: " +eventID +" "), serverResponse);
			break;
		case ADMIN_GET_BOOKING:
			clientId=promptForClientId(adminId.substring(0,3));
			Logger.ClientLog(adminId, " attempting to get Booking Schedule");
			serverResponse=Obj.getEventSchedule(clientId);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId,"View Booking "," ClientId: "+ clientId + " ", serverResponse);
			break;
		case ADMIN_CANCEL_BOOKING:
			clientId=promptForClientId(adminId.substring(0,3));
			eventType=promptForEventType();
			eventID=promptForEventId();
			Logger.ClientLog(adminId, " attempting to cancel an event");
			serverResponse=Obj.cancelTicket(clientId, eventID);
			System.out.println(serverResponse);
			Logger.ClientLog(adminId,"CancelEvent", ("clientID: "+ clientId +" EventID: " +eventID +" "), serverResponse);
			break;
		case ADMIN_EXCHANGE_TICKET:
			clientId=promptForClientId(adminId.substring(0,3));
			System.out.println("Please enter the old event you attempt to replace");
			eventType=promptForEventType();
			eventID=promptForEventId();
			System.out.println("Please enter the new event that you attempt to acquire");
			String newEventType=promptForEventType();
			String newEventID=promptForEventId();
			Logger.ClientLog(adminId, "attempting to exchange tickets");
			serverResponse=Obj.exchangeTickets(clientId, eventID, newEventID,newEventType);
			System.out.print(serverResponse);
			Logger.ClientLog(adminId, "exchange ticket "," oldEventID "+ eventID+" oldEventType "+ eventType+ " newEventID "+ newEventID+ " newEventType "+ newEventType, serverResponse);
			break;	
		case ADMIN_LOGOUT:
			logOn=false;
			Logger.ClientLog(adminId, " attempting to log out");
			start();
			break;
		}
		if(logOn) {
			Admin(adminId);
			
		}
	}
	private static void printMenu(int userType) {
		System.out.println("****************************************************");
		System.out.println("Please enter an option");
		if(userType==PARTICIPANT_TYPE) {
			System.out.println("1.Book Event");
			System.out.println("2.Get Event Schedule");
			System.out.println("3.Cancel Event");
			System.out.println("4.Exchange ticket");
			System.out.println("5.Log Out");
		}
		else if(userType==ADMIN_TYPE){
			System.out.println("1.Add Event");
			System.out.println("2.Remove Event");
			System.out.println("3.List Event Availability");
			System.out.println("4.Book Event");
			System.out.println("5.Get Event Schedule");
			System.out.println("6.Cancel Event");
			System.out.println("7.Exchange ticket");
			System.out.println("8.Log Out");
		}
	}
	private static String promptForEventType() {
		System.out.println("****************************************************");
		System.out.println("Please choose and event type below");
		System.out.println("1.Art gallery");
		System.out.println("2.Concert");
		System.out.println("3.Theater");
		try {
			switch(input.nextInt()) {
				case 1:
					return "Art gallery"; 
				case 2:
					return "Concert"; 
				case 3:
					return "Theater"; 
			}
		}catch(InputMismatchException e) {
			input.next();
			return promptForEventType();
			
		}
		
		return promptForEventType();
	}
	private static int promptForCapacity() {
		System.out.println("****************************************************");
		System.out.println("Please enter the booking capacity");
		return input.nextInt();
	}
	private static String promptForEventId() {
		System.out.println("****************************************************");
		System.out.println("Please enter the EventID (e.g MTLA124121(dd/mm/yy))");
		String eventid=input.next().trim().toUpperCase();
		
		if(eventid.length()==10) {
			if(eventid.substring(0,3).equalsIgnoreCase("MTL")||eventid.substring(0,3).equalsIgnoreCase("TOR")||eventid.substring(0,3).equalsIgnoreCase("VAN")) {
				if(eventid.substring(3,4).equalsIgnoreCase("M")||eventid.substring(3,4).equalsIgnoreCase("E")||eventid.substring(3,4).equalsIgnoreCase("A")) {
					return eventid;
				}
			}
		}
		return promptForEventId();
	}
	private static String promptForClientId(String serverAcro) {
		System.out.println("Please enter a customerID( within " + serverAcro + " server):");
		String userId=input.next().trim().toUpperCase();
		if(checkUserType(userId)!=PARTICIPANT_TYPE||!userId.substring(0,3).equals(serverAcro)) {
			return promptForClientId(serverAcro);
		}
		else
			return userId;
	}
	//validate id and check user type
	private static int checkUserType(String userId) {
		if(userId.length()==8) {
			if(userId.substring(0, 3).equalsIgnoreCase("MTL")||userId.substring(0, 3).equalsIgnoreCase("TOR")||userId.substring(0, 3).equalsIgnoreCase("VAN")) {
				if(userId.substring(3,4).equalsIgnoreCase("A"))
					return ADMIN_TYPE;
				else if(userId.substring(3,4).equalsIgnoreCase("P")) {
					return PARTICIPANT_TYPE;
				}
			}
		}
		return 0;
	}
	private static String getServerID(String userID) {
        String branch= userID.substring(0,3);
		if (branch.equalsIgnoreCase("MTL")) {
			Obj=montrealService.getPort(DTRSWebServices.class); 
			return branch;
        } else if (branch.equalsIgnoreCase("TOR")) {
        	Obj=torontoService.getPort(DTRSWebServices.class); 
            return branch;
        } else if (branch.equalsIgnoreCase("VAN")) {
        	Obj=vancouverService.getPort(DTRSWebServices.class); 
            return branch;
        }
        return "1";
    }
}
