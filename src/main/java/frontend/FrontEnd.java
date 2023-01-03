package frontend;

import model.Response;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface FrontEnd {
    Response addReservationSlot(String eventID, String eventType, int capacity);
    Response removeReservationSlot(String eventID, String eventType);
    Response listReservationSlotAvailable(String eventType);
    Response reserveTicket(String participantID, String eventID, String eventType);
    Response getEventSchedule(String participantID);
    Response cancelTicket(String participantID, String eventID);
    Response exchangeTickets(String participantID, String eventID, String newEventId, String newEventType);
}
