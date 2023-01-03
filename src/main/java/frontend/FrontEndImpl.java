package frontend;

import model.Response;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Logger;


@WebService(endpointInterface = "frontend.FrontEnd")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class FrontEndImpl implements FrontEnd {
    public String sequencerAddress;
    public int sequencerPort;
    public String city;

    private final Logger logger = Logger.getLogger(FrontEndImpl.class.getName());

    public FrontEndImpl() {
        logger.info("Starting FrontEnd...");
    }

    public void setSequencerAddress(String sequencerAddress) {
        this.sequencerAddress = sequencerAddress;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setSequencerPort(int sequencerPort) {
        this.sequencerPort = sequencerPort;
    }

    @WebMethod
    public Response addReservationSlot(String eventID, String eventType, int capacity) {
        String message = String.format("addReservationSlot-%s-%s,%s,%d", city, eventID, eventType, capacity);
        return processRequest(message);
    }

    @WebMethod
    public Response removeReservationSlot(String eventID, String eventType) {
        logger.info("");
        String message = String.format("removeReservationSlot-%s-%s,%s", city, eventID, eventType);
        return processRequest(message);
    }

    @WebMethod
    public Response listReservationSlotAvailable(String eventType) {
        logger.info("Listing reservation slog available");
        String message = String.format("listReservationSlotAvailable-%s-%s", city, eventType);
        return processRequest(message);
    }

    @WebMethod
    public Response reserveTicket(String participantID, String eventID, String eventType) {
        logger.info("Reserving ticket");
        String message = String.format("reserveTicket-%s-%s,%s,%s", city, participantID, eventID, eventType);
        Response response = processRequest(message);
        logger.info("" + response.isSuccessful());
        logger.info(response.getMessage());
        return response;
    }

    @WebMethod
    public Response getEventSchedule(String participantID) {
        String message = String.format("getEventSchedule-%s-%s", city, participantID);
        return processRequest(message);
    }

    @WebMethod
    public Response cancelTicket(String participantID, String eventID) {
        String message = String.format("cancelTicket-%s-%s,%s", city, participantID, eventID);
        return processRequest(message);
    }

    @WebMethod
    public Response exchangeTickets(String participantID, String eventID, String newEventId, String newEventType) {
        String message = String.format("exchangeTickets-%s-%s,%s,%s,%s", city, participantID, eventID, newEventId, newEventType);
        return processRequest(message);
    }

    private void sendMessageToSequencer(String message) throws UnknownHostException {
        Unicast.build(message, Unicast.createAddress(sequencerAddress), sequencerPort).send();
    }

    private Response processRequest(String message) {
        RMListener rmListener = RMListener.build(8999);

        try {
            sendMessageToSequencer(message);
            logger.info("Message sent... now listening");
            rmListener.listen();
            String responseMessage = rmListener.extractResponse();
            rmListener.notifyRMBadResult(message);

            logger.info(responseMessage);
            if (responseMessage.length() > 1) {
                String[] response = responseMessage.split(":");
                return new Response(response[1], response[0].equals("1"));
            }

            return new Response(responseMessage, responseMessage.equals("1"));
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }

        return null;
    }
}
