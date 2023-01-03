package replicas.philippe.Server;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

import replicas.philippe.model.Response;

@WebService
@SOAPBinding(style = Style.RPC)
public interface Participant {
    Response reserveTicket(String participantID, String eventID, String eventType);
    Response getEventSchedule(String participantID);
    Response cancelTicket(String participantID, String eventID);
    Response exchangeTickets(String participantID, String eventID, String newEventId, String newEventType);
}
