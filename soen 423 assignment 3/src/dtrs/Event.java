package dtrs;
import java.util.ArrayList;
import java.util.List;

import dtrs.com.web.service.DTRSWebServicesImpl;
 
public class Event {
	public static final String EVENT_TIME_MORNING="Morning";
	public static final String EVENT_TIME_AFTERNOON="Afternoon";
	public static final String EVENT_TIME_EVENING="evening";
	public static final String ART_GALLERY="Art gallery";
	public static final String CONCERT="Concert";
	public static final String THEATER="Theater";
	public static final int FULL=-1;
	public static final int REGISTERED=0;
	public static final int ADDED=1;
	
	private String eventType;
	private String eventID;
	private String eventServer;
	private int eventCapacity;
	private String eventDate;
	private String eventTimeSlot;
	private List<String> RegisteredParticipants;
	
	
	
	public Event(String eventType,String eventID,int capacity) {
		this.eventID=eventID;
		this.eventType=eventType;
		this.eventCapacity=capacity;
		this.eventTimeSlot=EventTimeSlot(eventID);
		this.eventServer=EventServer(eventID);
		this.eventDate=EventDate(eventID);
		this.RegisteredParticipants=new ArrayList<>();
	}



	public static String EventDate(String eventID) {
		// TODO Auto-generated method stub
		return eventID.substring(4, 6) + "/" + eventID.substring(6, 8) + "/20" + eventID.substring(8, 10);
    
	}



	public static String EventServer(String eventID) {
		// TODO Auto-generated method stub
		if (eventID.substring(0, 3).equalsIgnoreCase("MTL")) {
            return DTRSWebServicesImpl.Server_Montreal;
        } else if (eventID.substring(0, 3).equalsIgnoreCase("TOR")) {
            return DTRSWebServicesImpl.Server_Toronto;
        } else {
            return DTRSWebServicesImpl.Server_Vancouver;
        }
	}
	
	public static String EventTimeSlot(String eventID) {
		// TODO Auto-generated method stub
		if (eventID.substring(3, 4).equalsIgnoreCase("M")) {
            return EVENT_TIME_MORNING;
        } else if (eventID.substring(3, 4).equalsIgnoreCase("A")) {
            return EVENT_TIME_AFTERNOON;
        } else {
            return EVENT_TIME_EVENING;
        }
    
	}
	public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventID() {
        return eventID;
    }

    public void setEventID(String eventID) {
        this.eventID = eventID;
    }

    public String getEventServer() {
        return eventServer;
    }

    public void setEventServer(String eventServer) {
        this.eventServer = eventServer;
    }

    public int getEventCapacity() {
        return eventCapacity;
    }

    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    public int getEventRemainCapacity() {
        return eventCapacity - RegisteredParticipants.size();
    }
    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getEventTimeSlot() {
        return eventTimeSlot;
    }

    public void setEventTimeSlot(String eventTimeSlot) {
        this.eventTimeSlot = eventTimeSlot;
    }

    public boolean isFull() {
        return getEventCapacity() == RegisteredParticipants.size();
    }

    public List<String> getRegisteredClientIDs() {
        return RegisteredParticipants;
    }

    public void setRegisteredClientsIDs(List<String> registeredClientsIDs) {
        this.RegisteredParticipants = registeredClientsIDs;
    }
    public int addRegisteredClientID(String registeredClientID) {
        if (isFull()) {
        	return FULL;
        }else if (RegisteredParticipants.contains(registeredClientID)) {
                return REGISTERED;
            } else {
                RegisteredParticipants.add(registeredClientID);
                return ADDED;
            }
         
    }

    public boolean removeRegisteredClientID(String registeredClientID) {
        if(RegisteredParticipants.contains(registeredClientID)) {
        	return RegisteredParticipants.remove(registeredClientID);
        }
        else {
        	return false;
        }
    }

    @Override
    public String toString() {
        return " (" + getEventID() + ") in the " + getEventTimeSlot() + " of " + getEventDate() + " Total[Remaining] Capacity: " + getEventCapacity() + "[" + getEventRemainCapacity() + "]";
    }
}
	

