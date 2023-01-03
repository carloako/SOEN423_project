package replicas.philippe.Server;

import replicas.philippe.model.Response;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

@WebService
@SOAPBinding(style = Style.RPC)
public interface Admin {
    Response addReservationSlot(String eventID, String eventType, int capacity);
    Response removeReservationSlot(String eventID, String eventType);
    Response listReservationSlotAvailable(String eventType);
}
