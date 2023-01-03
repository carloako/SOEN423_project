package dtrs.com.web.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
//import java.rmi.RemoteException;
//import java.rmi.server.*;
//import DTRS.CORBAinterfacePOA;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import dtrs.ClientObject;
import dtrs.Event;
import dtrs.Logger;

//import org.omg.CORBA.ORB;

@WebService(endpointInterface="dtrs.com.web.service.DTRSWebServices")
@SOAPBinding(style=Style.RPC)

public class DTRSWebServicesImpl implements DTRSWebServices{
	public static final int Montreal_server_udp_port=18080;
	public static final int Toronto_server_udp_port=18081;
	public static final int Vancouver_server_udp_port=18082;
	public static final String Server_Montreal="MONTREAL";
	public static final String Server_Toronto="TORONTO";
	public static final String Server_Vancouver="VANCOUVER";
	private String serverID;
	private String serverName;
	//HashMap of <EventType,<eventID , Event>>
	private Map<String,Map<String,Event>> events;
	//participantID,event_type,eventID
	private Map<String,Map<String,List<String>>> clientEvents;
	//ClientID, Client
	private Map<String,ClientObject> serverClients;
	
	
	//interface constructor
	public DTRSWebServicesImpl(String ServerId,String servername) {
		super();
		this.serverID=ServerId;
		this.serverName=servername;
		events=new ConcurrentHashMap<>();
		events.put(Event.ART_GALLERY,new ConcurrentHashMap<>());
		events.put(Event.CONCERT,new ConcurrentHashMap<>());
		events.put(Event.THEATER,new ConcurrentHashMap<>());
		clientEvents=new ConcurrentHashMap<>();
		serverClients=new ConcurrentHashMap<>();
		addTestData();
	}
	//interface methods
	private void addTestData() {
		ClientObject testA= new ClientObject(serverID+"A1000");
		ClientObject testP= new ClientObject(serverID+"P1000");
		serverClients.put(testA.getID(), testA);
		serverClients.put(testP.getID(), testP);
		clientEvents.put(testP.getID(), new ConcurrentHashMap<>());
		
		
		 Event sampleArt = new Event(Event.ART_GALLERY, serverID + "A101010", 5);
	     sampleArt.addRegisteredClientID(testP.getID());
         clientEvents.get(testP.getID()).put(sampleArt.getEventType(), new ArrayList<>());
         clientEvents.get(testP.getID()).get(sampleArt.getEventType()).add(sampleArt.getEventID());

         Event sampleConcert = new Event(Event.CONCERT, serverID + "E201010", 15);
         sampleConcert.addRegisteredClientID(testP.getID());
         clientEvents.get(testP.getID()).put(sampleConcert.getEventType(), new ArrayList<>());
         clientEvents.get(testP.getID()).get(sampleConcert.getEventType()).add(sampleConcert.getEventID());

	        Event sampleTheater = new Event(Event.THEATER, serverID + "M301010", 20);
	        sampleTheater.addRegisteredClientID(testP.getID());
	        clientEvents.get(testP.getID()).put(sampleTheater.getEventType(), new ArrayList<>());
	        clientEvents.get(testP.getID()).get(sampleTheater.getEventType()).add(sampleTheater.getEventID());

	        events.get(Event.ART_GALLERY).put(sampleArt.getEventID(), sampleArt);
	        events.get(Event.CONCERT).put(sampleConcert.getEventID(), sampleConcert);
	        events.get(Event.THEATER).put(sampleTheater.getEventID(), sampleTheater);
		
		
		
		
		
	}
	private static int getServerPort(String locationAcro) {
		switch(locationAcro) {
		case "MTL": return Montreal_server_udp_port;
		case "TOR": return Toronto_server_udp_port;
		case "VAN": return Vancouver_server_udp_port;
		default: return 1;
		}
			
	}
	//participant remote method
	@Override
	
	public String reserveTicket(String participantID, String eventID, String eventType){
		// TODO Auto-generated method stub
		String response;
		
		//checks if participant is new
		if(!serverClients.containsKey(participantID)) {
			addNewParticipantsToClients(participantID);
			
		}//check if event exist in the correct server
		if(events.get(eventType).containsKey(eventID)) {
			//checks if event exist in the server
			if(Event.EventServer(eventID).equalsIgnoreCase(serverName)){
				Event bookedEvent=events.get(eventType).get(eventID);
				if(!bookedEvent.isFull()) {
					if(clientEvents.containsKey(participantID)) {
						if(clientEvents.get(participantID).containsKey(eventType)) {
							if(!clientEvents.get(participantID).get(eventType).contains(eventID)) {
								clientEvents.get(participantID).get(eventType).add(eventID);
							}else {
								response="Failed: Event " + eventID+ "is already booked by participant "+ participantID;
								try {
	                                Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", response);
	                            } catch (IOException e) {
	                                e.printStackTrace();
	                            }
	                            return response;
							}
						}else {
							List<String>temp= new ArrayList<>();
							temp.add(eventID);
							clientEvents.get(participantID).put(eventType, temp);
						}
					}else {
						Map<String,List<String>> temp= new ConcurrentHashMap<>();
						List<String>temp2= new ArrayList<>();
						temp2.add(eventID);
						temp.put(eventType, temp2);
						clientEvents.put(participantID, temp);
					}	
					
						if(events.get(eventType).get(eventID).addRegisteredClientID(participantID) == Event.ADDED) {
							response= "Success: Event "+ eventID + " booked successfully";
						}
						else if(events.get(eventType).get(eventID).addRegisteredClientID(participantID) == Event.FULL){
							response="Failed: Event "+ eventID + " is Full";
						}
						else {
							response="Failed: Cannot Add Participant "+ participantID+ " to Event " + eventID + "Already registered";
						}
					try {
	                    Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", response);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                return response;
				}else {
					response="Failed: Event "+ eventID + " is full";
					try {
	                    Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", response);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                return response;
				}
			}
		}else {
				if(!exceedWeeklyLimit(participantID,eventID.substring(4))) {
					String serverResponse= sendUDPMessage(getServerPort(eventID.substring(0,3)),"bookEvent",participantID,eventType,eventID);
					if(serverResponse.startsWith("Success: ")) {
						if(clientEvents.get(participantID).containsKey(eventType)) {
							clientEvents.get(participantID).get(eventType).add(eventID);
							try {
			                    Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", serverResponse);
			                } catch (IOException e) {
			                    e.printStackTrace();
			                }
							return serverResponse;
						}else {
							List<String>temp= new ArrayList<>();
							temp.add(eventID);
							clientEvents.get(participantID).put(eventType, temp);
							response="Failed: The event does not exist";
							return response;
							
						}
					}else {
						try {
						
		                 Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", serverResponse);
		             } catch (IOException e) {
		                 e.printStackTrace();
		             }
		             return serverResponse;
					}		
					
					
				} else {
	                response = "Failed: You Cannot Book Event in Other Servers For This Week(Max Weekly Limit = 3)";
	                try {
	                    Logger.ServerLog(serverID, participantID, " RMI bookEvent ", " eventID: " + eventID + " eventType: " + eventType + " ", response);
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	                return response;
					
				}
				
				
		}
		response="Failed: The event does not exist";
		return response;
		
		
	}

	@Override
	
	public String getEventSchedule(String participantID){
		// TODO Auto-generated method stub
		String response;
		if(!serverClients.containsKey(participantID)) {
			addNewParticipantsToClients(participantID);
			response="Booking Schedule is Empty For "+ participantID+ " as he is a new Participant";
			try {
                Logger.ServerLog(serverID, participantID, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
		}
		Map<String,List<String>>eventMap=clientEvents.get(participantID);
		if(eventMap.size()==0) {
			response="Booking Schedule is Empty For "+ participantID;
			try {
                Logger.ServerLog(serverID, participantID, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
		}
		StringBuilder sb = new StringBuilder();
        for (String eventType :eventMap.keySet()) {
            sb.append(eventType + ":\n");
            for (int i=0 ;i<eventMap.get(eventType).size();i++) {
            	String eventID=eventMap.get(eventType).get(i);
            	sb.append(eventID + " || ");
            }
            sb.append("\n--------------------------------\n");
        }
        response = sb.toString();
        try {
            Logger.ServerLog(serverID, participantID, " RMI getBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;	
	}

	@Override
	
	public String cancelTicket(String participantID, String eventID) {
		// TODO Auto-generated method stub
		String response;
		
		//check if Event is connected the same server... else it will send a udp message to the other server to cancel
		if (Event.EventServer(eventID).equals(serverName)) {
			//checks 
            if (participantID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(participantID)) {
                    addNewParticipantsToClients(participantID);
                    response = "Failed: You " + participantID + " Are Not Registered in " + eventID;
                    try {
                        Logger.ServerLog(serverID, participantID, " RMI cancelEvent ", " eventID: " + eventID  + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                } 
                else {
                	
                	for (String eventType :events.keySet()) {
                		if(!clientEvents.get(participantID).containsKey(eventType)) {
                			continue;
                		}
                		else {
	                		 if (clientEvents.get(participantID).get(eventType).contains(eventID)) {
	                			 clientEvents.get(participantID).get(eventType).remove(eventID);
	                			 events.get(eventType).get(eventID).removeRegisteredClientID(participantID);
	                             response="Success: Event " + eventID + " Cancelled for "+ participantID;
	                             try {
	                                 Logger.ServerLog(serverID, participantID, " RMI cancelEvent ", " eventID: " + eventID+ " Event Type " + eventType  + " ", response);
	                             } catch (IOException e) {
	                                 e.printStackTrace();
	                             }
	                             return response;
	                		 }
	                		 
                		}
                	}
                	response="Failed: Participant" + participantID + " is not registered in "+ eventID;
            		try {
                         Logger.ServerLog(serverID, participantID, " RMI cancelEvent ", " eventID: " + eventID+ " ", response);
                    } 
            		catch (IOException e) {
                         e.printStackTrace();
                    }
            		return response;
            		}
            		 	
                	
                       
            
            }else {
            	
            	for (String eventType :events.keySet()) {
            		 if (events.get(eventType).get(eventID).removeRegisteredClientID(participantID)){
            			 
                         response="Success: Event " + eventID + " Cancelled for "+ participantID;
                         try {
                             Logger.ServerLog(serverID, participantID, " RMI cancelEvent ", " eventID: " + eventID+ " Event Type " + eventType  + " ", response);
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                         return response;
            		 }
            		
                }
            	
            		response="Failed: Participant" + participantID + " is not registered in "+ eventID;
            		 try {
                         Logger.ServerLog(serverID, participantID, " RMI cancelEvent ", " eventID: " + eventID+ " ", response);
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
            		 return response;         
            }       
		}
		//send a udp message to the other server to cancel as event is not in the server
		else {
            if(participantID.substring(0, 3).equals(serverID)) {
                if (!serverClients.containsKey(participantID)) {
                    addNewParticipantsToClients(participantID);
                } else {
                	for (String eventType :events.keySet()) {
	                    if (clientEvents.get(participantID).get(eventType).contains(eventID)){
	                        return sendUDPMessage(getServerPort(eventID.substring(0, 3)), "cancelEvent", participantID, eventType, eventID);
	                    }
                	}
                }
            }
            }
            return "Failed: You " + participantID + " Are Not Registered in " + eventID;
		
	
	}
	//admin remote method
	@Override
	
	public String addReservationSlot(String eventID, String eventType, int capacity)  {
		// TODO Auto-generated method stub
		String response;
		if(events.get(eventType).containsKey(eventID)) {
			response="Failed: Event Already Exists";
			try {
				Logger.ServerLog(serverID, "null","RMI add ReservationSlot", "eventID: "+ eventID+ " event Type: "+ eventType + " Capacity " + capacity+" ",response );
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return response;
		}
		if(Event.EventServer(eventID).equals(serverName)) {
			Event event= new Event(eventType,eventID,capacity);
			Map<String,Event>eventHashMap=events.get(eventType);
			eventHashMap.put(eventID, event);
			events.put(eventType, eventHashMap);;
			response="Success: Event "+ eventID + "added succesfully";
			try {
				Logger.ServerLog(serverID, "null","RMI add ReservationSlot", "eventID: "+ eventID+ " event Type: "+ eventType + " Capacity " + capacity+" ",response );
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return response;
		}else
			response="Failed: Cannot Add Event to server other than "+ serverName;
			try {
				Logger.ServerLog(serverID, "null","RMI add ReservationSlot", "eventID: "+ eventID+ " event Type: "+ eventType + " Capacity " + capacity+" ",response );
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return response;	
	}
	@Override
	
	public String removeReservationSlot(String eventID, String eventType) {
		// TODO Auto-generated method stub
		String response;
		if (Event.EventServer(eventID).equals(serverName)){
			if(events.get(eventType).containsKey(eventID)) {
				List<String> clients=events.get(eventType).get(eventID).getRegisteredClientIDs();
				events.get(eventType).remove(eventID);
				addParticipantToNextSameEvent(eventID,eventType,clients);
				response="Success:Event Removed Sucessfully";
			
				try {
					Logger.ServerLog(serverID, "null","RMI Remove reservation slot", "eventID: "+ eventID+ " event Type: "+ eventType +" ",response );
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				return response;
			}
			else {
				response="Failed: Event"+ eventID+ "Does not Exist";
				try {
					Logger.ServerLog(serverID, "null","RMI Remove reservation slot", "eventID: "+ eventID+ " event Type: "+ eventType +" ",response );
				}
				catch(IOException e) {
					e.printStackTrace();
				}
				return response;
			}
		}else {
			response="Failed: Cannot remove Event from Server other than"+ serverName;
			try {
				Logger.ServerLog(serverID, "null","RMI Remove reservation slot", "eventID: "+ eventID+ " event Type: "+ eventType +" ",response );
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			return response;
		}
		
	}

	@Override
	
	public String listReservationSlotAvailable(String eventType) {
		// TODO Auto-generated method stub
		String response;
		Map<String,Event>eventMap= events.get(eventType);
		StringBuilder sb=new StringBuilder();
		sb.append(serverName+ " server "+ eventType+ " :\n");
		if(eventMap.size()==0) {
			sb.append("No Event of Type "+ eventType);
		}
		else {
			for(Event event:eventMap.values()) {
				sb.append(event.toString() + " |");
			}
			sb.append("\n---------------------------------\n");
		}
		String s1,s2;
		
	
		if (serverID.equalsIgnoreCase("MTL")) {
			s1=sendUDPMessage(Toronto_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
			s2=sendUDPMessage(Vancouver_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
		}
		else if (serverID.equalsIgnoreCase("TOR")) {
			s1=sendUDPMessage(Montreal_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
			s2=sendUDPMessage(Vancouver_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
		}	
		else if(serverID.equalsIgnoreCase("VAN")){  
			s1=sendUDPMessage(Montreal_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
			s2=sendUDPMessage(Toronto_server_udp_port,"listReservationSlotAvailable","null",eventType,"null");
		}
		else {
			s1=null;s2=null;
		}
		sb.append(s1).append(s2);
		response=sb.toString();
		try {
            Logger.ServerLog(serverID, "null", " RMI listEventAvailability ", " eventType: " + eventType + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
	}
	
	//udp methods
	
	public String removeEventUDP(String eventID,String eventType,String participantID) {
		if (!serverClients.containsKey(participantID)) {
            addNewParticipantsToClients(participantID);
            return "Failed: You " + participantID + " Are Not Registered in " + eventID;
        } else {
            if (clientEvents.get(participantID).get(eventType).remove(eventID)) {
                return "Success: Event " + eventID + " Was Removed from " + participantID + " Schedule";
            } else {
                return "Failed: You " + participantID + " Are Not Registered in " + eventID;
            }
        }
	}
	
	public String listEventAvailabilityUDP(String eventType) {
		 Map<String, Event> eventMap = events.get(eventType);
	        StringBuilder sb = new StringBuilder();
	        sb.append(serverName + " Server " + eventType + ":\n");
	        if (events.size() == 0) {
	            sb.append("No Events of Type " + eventType);
	        } else {
	            for (Event event :eventMap.values()) {
	                sb.append(event.toString() + " || ");
	            }
	        }
	        sb.append("\n=====================================\n");
	        return sb.toString();
	}
	private String sendUDPMessage(int serverPort,String method,String participantID,String eventType,String eventID) {
		DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + participantID + ";" + eventType + ";" + eventID;
        try {
            Logger.ServerLog(serverID, participantID, " UDP request sent " + method + " ", " eventID: " + eventID + " eventType: " + eventType + " ", " ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message, dataFromClient.length(), aHost, serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];
        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        } finally {
            if (aSocket != null)
                aSocket.close();
        }
        try {
            Logger.ServerLog(serverID, participantID, " UDP reply received" + method + " ", " eventID: " + eventID + " eventType: " + eventType + " ", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
	}
	private String getNextSameEvent(Set<String>keySet,String eventType,String eventID) {
		 List<String> sortedIDs = new ArrayList<String>(keySet);
	        sortedIDs.add(eventID);
	        Collections.sort(sortedIDs, new Comparator<String>() {
	            @Override
	            public int compare(String ID1, String ID2) {
	                Integer timeSlot1 = 0;
	                switch (ID1.substring(3, 4).toUpperCase()) {
	                    case "M":
	                        timeSlot1 = 1;
	                        break;
	                    case "A":
	                        timeSlot1 = 2;
	                        break;
	                    case "E":
	                        timeSlot1 = 3;
	                        break;
	                }
	                Integer timeSlot2 = 0;
	                switch (ID2.substring(3, 4).toUpperCase()) {
	                    case "M":
	                        timeSlot2 = 1;
	                        break;
	                    case "A":
	                        timeSlot2 = 2;
	                        break;
	                    case "E":
	                        timeSlot2 = 3;
	                        break;
	                }
	                Integer date1 = Integer.parseInt(ID1.substring(8, 10) + ID1.substring(6, 8) + ID1.substring(4, 6));
	                Integer date2 = Integer.parseInt(ID2.substring(8, 10) + ID2.substring(6, 8) + ID2.substring(4, 6));
	                int dateCompare = date1.compareTo(date2);
	                int timeSlotCompare = timeSlot1.compareTo(timeSlot2);
	                if (dateCompare == 0) {
	                	return ((timeSlotCompare == 0) ? dateCompare : timeSlotCompare);
	                } else {
	                    return dateCompare;
	                }
	            }
	        });
	        int index = sortedIDs.indexOf(eventID) + 1;
	        for (int i = index; i < sortedIDs.size(); i++) {
	            if (!events.get(eventType).get(sortedIDs.get(i)).isFull()) {
	                return sortedIDs.get(i);
	            }
	        }
	        return "Failed";
	    
	                
	}
	private boolean exceedWeeklyLimit(String participantID,String eventDate) {
		int limit = 0;
        for (int i = 0; i < 3; i++) {
            List<String> registeredIDs = new ArrayList<>();
            switch (i) {
                case 0:
                    if (clientEvents.get(participantID).containsKey(Event.ART_GALLERY)) {
                        registeredIDs = clientEvents.get(participantID).get(Event.ART_GALLERY);
                    }
                    break;
                case 1:
                    if (clientEvents.get(participantID).containsKey(Event.CONCERT)) {
                        registeredIDs = clientEvents.get(participantID).get(Event.CONCERT);
                    }
                    break;
                case 2:
                    if (clientEvents.get(participantID).containsKey(Event.THEATER)) {
                        registeredIDs = clientEvents.get(participantID).get(Event.THEATER);
                    }
                    break;
            }
            for (String eventID :
                    registeredIDs) {
                if (eventID.substring(6, 8).equals(eventDate.substring(2, 4)) && eventID.substring(8, 10).equals(eventDate.substring(4, 6))) {
                    int week1 = Integer.parseInt(eventID.substring(4, 6)) / 7;
                    int week2 = Integer.parseInt(eventDate.substring(0, 2)) / 7;
//                    int diff = Math.abs(day2 - day1);
                    if (week1 == week2) {
                        limit++;
                    }
                }
                if (limit == 3)
                    return true;
            }
        }
        return false;
	}
	private void addParticipantToNextSameEvent(String eventID,String eventType,List<String> registeredClients) {
		for(String participantID:registeredClients) {
			if (participantID.substring(0, 3).equals(serverID)) {
                clientEvents.get(participantID).get(eventType).remove(eventID);
                String nextSameEventResult = getNextSameEvent(events.get(eventType).keySet(), eventType, eventID);
                if (nextSameEventResult.equals("Failed")) {
                    return;
                } else {
                    reserveTicket(participantID, nextSameEventResult, eventType);
                }
            } else {
                sendUDPMessage(getServerPort(participantID.substring(0, 3)), "removeEvent", participantID, eventType, eventID);
            }
        }	
	}
	public Map<String,Map<String,Event>>getAllEvents(){
		return events;
	}
	 public Map<String, Map<String, List<String>>> getClientEvents() {
	        return clientEvents;
	 }
	public Map<String, ClientObject> getServerClients() {
	        return serverClients;
    }
    public void addNewEvent(String eventID, String eventType, int capacity) {
        Event sampleConf = new Event(eventType, eventID, capacity);
        events.get(eventType).put(eventID, sampleConf);
    }
    public void addNewParticipantsToClients(String participantID) {
        ClientObject newCustomer = new ClientObject(participantID);
        serverClients.put(participantID, newCustomer);
        clientEvents.put(participantID, new ConcurrentHashMap<>());
        
    }
    
	@Override
	public String exchangeTickets(String participantID, String eventID, String new_eventID, String new_eventType) {
		// TODO Auto-generated method stub
		String response;
		
		//checks if client is registered to the server
		if(!serverClients.containsKey(participantID)) {
			response= "Failed: You" + participantID+ " Are Not Registed in" + eventID + "as you are not registed in the server";
			try {
				Logger.ServerLog(serverID, participantID, "CORBA exchange tickets", "eventID " + eventID+ "new eventID"+ new_eventID+ "new Event Type "+ new_eventType,response);
			}catch(IOException e) {
				e.printStackTrace();
			}
			return response;
		}
		//checks if client  is registed to event
		for (String eventType :events.keySet()) {
			
   		 	if (clientEvents.get(participantID).get(eventType).contains(eventID)) {
	   			 String bookResp="Failed: did not send book request for your new event"+ new_eventID;
	   			 String cancelResp="Failed: did not send cancel request for your new event"+ eventID;
	   			 
	   			 synchronized(this) {
	   				 if(onTheSameWeek(new_eventID.substring(4),eventID) &&! exceedWeeklyLimit(participantID,new_eventID.substring(4))) {
	   					 cancelResp = cancelTicket(participantID, eventID);
	                     if (cancelResp.startsWith("Success:")) {
	                         bookResp = reserveTicket(participantID, new_eventID, new_eventType);
	                     }
	   				 }else {
	   					 bookResp=reserveTicket(participantID,new_eventID,new_eventType);
	   					 if(bookResp.startsWith("Success:")) {
	   						 cancelResp=cancelTicket(participantID, eventID);
	   					 }
	   				 }
	   			 }
	   			 if(bookResp.startsWith("Success:")&&cancelResp.startsWith("Success:")) {
	   				 response = "Success: Event " + eventID + " swapped with " + new_eventID;
	             } else if (bookResp.startsWith("Success:") && cancelResp.startsWith("Failed:")) {
	                 cancelTicket(participantID, new_eventID);
	                 response = "Failed: Your oldEvent " + eventID + " Could not be Canceled reason: " + cancelResp;
	             } else if (bookResp.startsWith("Failed:") && cancelResp.startsWith("Success:")) {
	                 //hope this won't happen, but just in case.
	                 String resp1 = reserveTicket(participantID, new_eventID, eventType);
	                 response = "Failed: Your newEvent " + new_eventID + " Could not be Booked reason: " + bookResp + " And your old event Rolling back: " + resp1;
	             } else {
	                 response = "Failed: on Both newEvent " + new_eventID + " Booking reason: " + bookResp + " and oldEvent " + eventID + " Canceling reason: " + cancelResp;
	             }
	             try {
	                 Logger.ServerLog(serverID, participantID, " Webservice swapEvent ", " eventID: " + eventID + " eventType: " + eventType + " new_eventID: " + new_eventID + " new_eventType: " + new_eventType + " ", response);
	             } catch (IOException e) {
	                 e.printStackTrace();
	             }
	             return response;
         } 
         } response = "Failed: You " + participantID + " Are Not Registered in " + eventID ;
         try {
             Logger.ServerLog(serverID, participantID, " Webservice swapEvent ", " eventID: " + eventID + " eventType: " + null + " new_eventID: " + new_eventID + " new_eventType: " + new_eventType + " ", response);
         } catch (IOException e) {
             e.printStackTrace();
         }
         return response;
   	
		
   			 			 
  } 
	private boolean onTheSameWeek(String eventDate,String eventID) {
		 if (eventID.substring(6, 8).equals(eventDate.substring(2, 4)) && eventID.substring(8, 10).equals(eventDate.substring(4, 6))) {
             int week1 = Integer.parseInt(eventID.substring(4, 6)) / 7;
             int week2 = Integer.parseInt(eventDate.substring(0, 2)) / 7;
//             int diff = Math.abs(day2 - day1);
             return week1 == week2; 
                
             
         }else {
        	 return false;
         }
	}
}
