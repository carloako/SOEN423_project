package replicas.philippe.Server;

import replicas.philippe.model.Response;
import replicas.philippe.model.Message;
import replicas.philippe.model.Event;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@WebService(endpointInterface = "replicas.philippe.Server.Admin")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class AdminImpl implements Admin {

    private String region;
    private Logger logger;
    private Map<String, Integer> socketPorts;
    private Map<String, Map<String, Event>> eventRecords;

    private enum AdminRequestType {
        ADD_RESERVATION_SLOT("add reservation slot"),
        REMOVE_RESERVATION_SLOT("remove reservation slot"),
        LIST_AVAILABLE_SLOT("list available reservation slot");

        public final String value;

        AdminRequestType(String value) {
            this.value = value;
        }
    }

    public void setLogger(String logFileName) {
        if (logger == null) {
            this.logger = Logger.getLogger(this.getClass().getSimpleName());
            try {
                FileHandler fileHandler = new FileHandler(logFileName);
                logger.addHandler(fileHandler);
                fileHandler.setFormatter(new SimpleFormatter());
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void setSocketPorts(Map<String, Integer> socketPorts) {
        this.socketPorts = socketPorts;
    }

    public void setRegion(String region) {
        if (this.region == null) {
            this.region = region;
        }
    }

    public void setEventRecords(Map<String, Map<String, Event>> eventRecords) { this.eventRecords = eventRecords; }

    @Override
    public Response addReservationSlot(String eventID, String eventType, int capacity) {
        if (this.eventRecords.get(eventType).containsKey(eventID)) {
            this.logger.info(createLogMessage(
                    AdminRequestType.ADD_RESERVATION_SLOT.value,
                    String.format("%s, %s", eventID, eventType),
                    Message.FAILURE,
                    Message.RESERVATION_SLOT_ALREADY_TAKEN
            ));
            return new Response(Message.RESERVATION_SLOT_ALREADY_TAKEN, false);
        }

        Event anEvent = new Event(capacity, eventType);
        this.eventRecords.get(eventType).put(eventID, anEvent);
        this.logger.info(createLogMessage(
                AdminRequestType.ADD_RESERVATION_SLOT.value,
                String.format("%s, %s", eventID, eventType),
                Message.SUCCESS,
                Message.RESERVATION_SLOT_SUCCESSFUL
        ));
        return new Response(Message.RESERVATION_SLOT_SUCCESSFUL, true);
    }

    @Override
    public Response removeReservationSlot(String eventID, String eventType) {
        Event event = eventRecords.get(eventType).get(eventID);
        if (event == null) {
            this.logger.info(createLogMessage(
                    AdminRequestType.REMOVE_RESERVATION_SLOT.value,
                    String.format("%s, %s", eventID, eventType),
                    Message.FAILURE,
                    Message.EVENT_DOESNT_EXIST
            ));
            return new Response(Message.EVENT_DOESNT_EXIST, false);
        }
        if (event.getParticipants().size() > 0) {
            this.logger.info(createLogMessage(
                    AdminRequestType.REMOVE_RESERVATION_SLOT.value,
                    String.format("%s, %s", eventID, eventType),
                    Message.FAILURE,
                    Message.EVENT_ALREADY_RESERVED
            ));
            return new Response(Message.EVENT_ALREADY_RESERVED, false);
        }

        eventRecords.get(eventType).remove(eventID);
        this.logger.info(createLogMessage(
                AdminRequestType.REMOVE_RESERVATION_SLOT.value,
                String.format("%s, %s", eventID, eventType),
                Message.SUCCESS,
                Message.EVENT_REMOVED_SUCCESSFUL
        ));

        return new Response(Message.EVENT_REMOVED_SUCCESSFUL, true);
    }

    @Override
    public Response listReservationSlotAvailable(String eventType) {
        try {
            DatagramSocket udpClient = new DatagramSocket();
            InetAddress udpClientHost = InetAddress.getByName("localhost");

            String replyMessage = socketPorts.entrySet().stream()
                    .map(x -> {
                        if (x.getKey().equals(region)) {
                            return getAvailableEventSlot(eventType);
                        } else {
                            try {
                                String message = String.format("list-%s", eventType);
                                System.out.println(message);
                                byte[] udpBytesRequest = message.getBytes();
                                DatagramPacket request = new DatagramPacket(
                                        udpBytesRequest,
                                        message.length(),
                                        udpClientHost,
                                        x.getValue()
                                );

                                udpClient.send(request);

                                byte[] udpBytesReply = new byte[10000];
                                DatagramPacket reply = new DatagramPacket(udpBytesReply, udpBytesReply.length);

                                udpClient.receive(reply);
                                return new String(reply.getData()).trim();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return "";
                            }
                        }
                    })
                    .filter(x -> x.length() > 0)
                    .reduce("", this::recordFormatter);

            this.logger.info(createLogMessage(
                    AdminRequestType.LIST_AVAILABLE_SLOT.value,
                    eventType,
                    Message.SUCCESS,
                    replyMessage
            ));
            return new Response(replyMessage, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.logger.info(createLogMessage(
                AdminRequestType.LIST_AVAILABLE_SLOT.value,
                eventType,
                Message.FAILURE,
                Message.LIST_PROBLEM
        ));
        return new Response(Message.LIST_PROBLEM, false);
    }

    public String getAvailableEventSlot(String eventType) {
        return eventRecords.get(eventType).entrySet().stream()
                .map(event -> String.format("%s %d", event.getKey(), event.getValue().getCapacity()))
                .reduce("", this::recordFormatter);
    }

    private String recordFormatter(String acc, String event) {
        if (acc.equals(""))
            return event;
        else
            return acc + ", " + event;
    }

    private String createLogMessage(String requestType, String parameters, String requestState, String response) {
        Date date = new Date();
        String dateFormatted = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
        return String.format(
                "%s;%s;%s;%s;%s",
                dateFormatted,
                requestType,
                parameters,
                requestState,
                response
        );
    }
}
