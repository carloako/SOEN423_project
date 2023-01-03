package replicas.winyul.dtrs.com.web.service;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style=Style.RPC)
public interface DTRSWebServices  {

	
	//participant action
	public String reserveTicket (String participantID,String eventID,String eventType);
	
	public String getEventSchedule (String participantID) ;
	
	public String cancelTicket (String participantID,String eventID);
	
	public String exchangeTickets (String participantID,String eventID,String new_eventID,String new_eventType);
	
	//admin action
	public String addReservationSlot (String eventID,String eventType,int capacity) ;
	
	public String removeReservationSlot (String eventID,String eventType) ;
	
	public String listReservationSlotAvailable (String eventType) ;
}
