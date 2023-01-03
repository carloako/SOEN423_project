package replicas.philippe.Server;

import replicas.philippe.model.Response;
import replicas.philippe.model.Event;
import replicas.philippe.model.Message;

import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@WebService(endpointInterface = "replicas.philippe.Server.Participant")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public class ParticipantImpl implements Participant {
    protected String region;
    protected Logger logger;
    protected Map<String, Map<String, Event>> eventRecords;
    protected Map<String, Integer> socketPorts;

    private enum ParticipantRequestType {
        RESERVE_TICKET("reserve ticket"),
        CANCEL_TICKET("cancel ticket"),
        GET_SCHEDULE("get event schedule"),
        EXCHANGE_TICKETS("exchange ticket");

        public final String value;

        ParticipantRequestType(String value) {
            this.value = value;
        }
    }

    public void setRegion(String region) {
        if (this.region == null) {
            this.region = region;
        }
    }

    public void setSocketPorts(Map<String, Integer> socketPorts) { this.socketPorts = socketPorts; }

    public void setLogger(String logFileName) {
        this.logger = Logger.getLogger(this.getClass().getSimpleName());
        try {
            FileHandler fileHandler = new FileHandler(logFileName);
            logger.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void setEventRecords(Map<String, Map<String, Event>> eventRecords) { this.eventRecords = eventRecords; }

    @Override
    public Response reserveTicket(String participantID, String eventID, String eventType) {
        System.out.printf("Parameters: %s, %s, %s%n", participantID, eventID, eventType);
        System.out.println("Elements: " + eventRecords);
        Event event = eventRecords.get(eventType).getOrDefault(eventID, new Event(0, null));

        if (event.getEventType() == null) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.RESERVE_TICKET.value,
                    String.format("%s, %s, %s", participantID, eventID, eventType),
                    Message.FAILURE,
                    Message.EVENT_DOESNT_EXIST
            ));
            return new Response(Message.EVENT_DOESNT_EXIST, false);
        }
        System.out.println("Previous event capacity: " + event.getCapacity());

        if (event.getCapacity() <= 0) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.RESERVE_TICKET.value,
                    String.format("%s, %s, %s", participantID, eventID, eventType),
                    Message.FAILURE,
                    Message.RESERVE_TICKET_NO_CAPACITY
            ));

            return new Response(Message.RESERVE_TICKET_NO_CAPACITY, false);
        }

        if (event.getParticipants().contains(participantID)) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.RESERVE_TICKET.value,
                    String.format("%s, %s, %s", participantID, eventID, eventType),
                    Message.FAILURE,
                    Message.RESERVE_TICKET_ALREADY_RESERVED
            ));
            System.out.println("New event capacity: " + event.getCapacity());

            return new Response(Message.RESERVE_TICKET_ALREADY_RESERVED, false);
        }

        Response response = getEventSchedule(participantID);

        if (response.isSuccessful()) {
            String[] events = response.getMessage().split(",");
            int [] currentEvent = eventIdToWeek(eventID);
            long sameWeekEvents = Arrays.stream(events)
                    .filter(x -> x.length() > 0)
                    .map(ParticipantImpl::eventIdToWeek)
                    .filter(x -> x[0] == currentEvent[0] && x[1] == currentEvent[1])
                    .count();
            if (sameWeekEvents >= 3) {
                this.logger.info(createLogMessage(
                        ParticipantRequestType.RESERVE_TICKET.value,
                        String.format("%s, %s, %s", participantID, eventID, eventType),
                        Message.FAILURE,
                        Message.REACH_WEEKLY_RESERVE_LIMIT
                ));
                System.out.println("New event capacity: " + event.getCapacity());

                return new Response(Message.REACH_WEEKLY_RESERVE_LIMIT, false);
            }
        }

        long dailyCount = eventRecords.get(eventType).entrySet().stream()
                .filter(x -> x.getValue().getParticipants().contains(participantID))
                .filter(x -> getEventDate(x.getKey()).equals(getEventDate(eventID)))
                .count();

        if (dailyCount >= 1) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.RESERVE_TICKET.value,
                    String.format("%s, %s, %s", participantID, eventID, eventType),
                    Message.FAILURE,
                    Message.REACH_DAILY_RESERVE_LIMIT
            ));
            System.out.println("New event capacity: " + event.getCapacity());

            return new Response(Message.REACH_DAILY_RESERVE_LIMIT, false);
        }


        event.addParticipant(participantID);
        this.logger.info(createLogMessage(
                ParticipantRequestType.RESERVE_TICKET.value,
                String.format("%s, %s, %s", participantID, eventID, eventType),
                Message.SUCCESS,
                Message.RESERVE_TICKET_SUCCESS
        ));
        System.out.println("New event capacity: " + event.getCapacity());
        return new Response(Message.RESERVE_TICKET_SUCCESS, true);
    }

    @Override
    public Response getEventSchedule(String participantID) {
        try {
            DatagramSocket udpClient = new DatagramSocket();
            InetAddress udpClientHost = InetAddress.getByName("localhost");

            String replyMessage = socketPorts.entrySet().stream()
                    .map(x -> {
                        if (x.getKey().equals(region)) {
                            return this.getParticipantEvents(participantID);
                        } else {
                            try {
                                String message = String.format("schedule-%s", participantID);
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
                    .peek(System.out::println)
                    .reduce("",  this::eventsFormatter);

            this.logger.info(createLogMessage(
                    ParticipantRequestType.GET_SCHEDULE.value,
                    participantID,
                    Message.SUCCESS,
                    replyMessage
            ));
            return new Response(replyMessage, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.logger.info(createLogMessage(
                ParticipantRequestType.GET_SCHEDULE.value,
                participantID,
                Message.SUCCESS,
                Message.SCHEDULE_PROBLEM
        ));
        return new Response(Message.SCHEDULE_PROBLEM, false);
    }

    @Override
    public Response cancelTicket(String participantID, String eventID) {
        Event event = null;
        for (String evenType: eventRecords.keySet()) {
            Map<String, Event> records = eventRecords.get(evenType);
            if (records.containsKey(eventID)) {
                event = records.get(eventID);
                break;
            }
        }

        if (event == null) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.CANCEL_TICKET.value,
                    String.format("%s, %s", participantID, eventID),
                    Message.FAILURE,
                    Message.EVENT_DOESNT_EXIST
            ));
            return new Response(Message.EVENT_DOESNT_EXIST, false);
        }
        if (!event.getParticipants().contains(participantID)) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.CANCEL_TICKET.value,
                    String.format("%s, %s", participantID, eventID),
                    Message.FAILURE,
                    Message.CANCEL_TICKET_NO_RESERVATION
            ));
            return new Response(Message.CANCEL_TICKET_NO_RESERVATION, false);
        }

        event.removeParticipant(participantID);
        this.logger.info(createLogMessage(
                ParticipantRequestType.CANCEL_TICKET.value,
                String.format("%s, %s", participantID, eventID),
                Message.SUCCESS,
                Message.CANCEL_TICKET_SUCCESSFUL
        ));
        return new Response(Message.CANCEL_TICKET_SUCCESSFUL, true);
    }

    @Override
    public Response exchangeTickets(String participantID, String eventID, String newEventId, String newEventType) {
        String parameters = String.format("%s,%s,%s,%s", participantID, eventID, newEventId, newEventType);
        //        Check if old ticket is reserved
        final String[] oldEventType = new String[1];
        boolean isReserved = eventRecords.entrySet().stream().anyMatch(record -> {
            boolean hasReservation = record.getValue().entrySet().stream()
                    .filter(x -> x.getKey().equals(eventID))
                    .anyMatch(x -> x.getValue().getParticipants().contains(participantID));
            if (hasReservation) {
                oldEventType[0] = record.getKey();
            }
            return hasReservation;
        });


        if (!isReserved) {
            this.logger.info(createLogMessage(
                    ParticipantRequestType.EXCHANGE_TICKETS.value,
                    parameters,
                    Message.FAILURE,
                    Message.NO_EVENT_RESERVATION
            ));
            return new Response(Message.NO_EVENT_RESERVATION, false);
        }

//        Check if new ticket is available
        try {
            DatagramSocket udpClient = new DatagramSocket();
            InetAddress udpClientHost = InetAddress.getByName("localhost");
            String newCity = newEventId.substring(0, 3);

            try {
                String message = String.format("available-%s,%s", newEventType, newEventId);
                byte[] udpBytesRequest = message.getBytes();
                DatagramPacket request = new DatagramPacket(
                        udpBytesRequest,
                        message.length(),
                        udpClientHost,
                        socketPorts.get(newCity)
                );

                udpClient.send(request);

                byte[] udpBytesReply = new byte[10000];
                DatagramPacket reply = new DatagramPacket(udpBytesReply, udpBytesReply.length);

                udpClient.receive(reply);
                String replyMessage = new String(reply.getData()).trim();

//                If old ticket reserved and new ticket available
                System.out.println(replyMessage);
                if (!Boolean.parseBoolean(replyMessage)) {
                    this.logger.info(createLogMessage(
                            ParticipantRequestType.EXCHANGE_TICKETS.value,
                            parameters,
                            Message.FAILURE,
                            Message.NEW_TICKET_NOT_AVAILABLE
                    ));
                    return new Response(Message.NEW_TICKET_NOT_AVAILABLE, false);
                }

//                reserve new ticket
                String reserveMessage = String.format("reserve-%s,%s,%s", participantID, newEventType, newEventId);
                udpBytesRequest = reserveMessage.getBytes();
                request = new DatagramPacket(
                        udpBytesRequest,
                        reserveMessage.length(),
                        udpClientHost,
                        socketPorts.get(newCity)
                );

                udpClient.send(request);

                udpBytesReply = new byte[10000];
                reply = new DatagramPacket(udpBytesReply, udpBytesReply.length);

                udpClient.receive(reply);
                replyMessage = new String(reply.getData()).trim();

                if (!Boolean.parseBoolean(replyMessage)) {
                    this.logger.info(createLogMessage(
                            ParticipantRequestType.EXCHANGE_TICKETS.value,
                            parameters,
                            Message.FAILURE,
                            Message.NEW_TICKET_FAILED_RESERVATION
                    ));
                    return new Response(Message.NEW_TICKET_FAILED_RESERVATION, false);
                }

//                if successful cancel old ticket
                Response response = this.cancelTicket(participantID, eventID);

                if (response.isSuccessful()) {
                    this.logger.info(createLogMessage(
                            ParticipantRequestType.EXCHANGE_TICKETS.value,
                            parameters,
                            Message.SUCCESS,
                            Message.TICKET_EXCHANGE_SUCCESSFUL
                    ));
                    return new Response(Message.TICKET_EXCHANGE_SUCCESSFUL, true);
                }

                String cancelMessage = String.format("cancel-%s,%s", participantID, newEventId);
                udpBytesRequest = cancelMessage.getBytes();
                request = new DatagramPacket(
                        udpBytesRequest,
                        cancelMessage.length(),
                        udpClientHost,
                        socketPorts.get(newCity)
                );

                udpClient.send(request);

                udpBytesReply = new byte[10000];
                reply = new DatagramPacket(udpBytesReply, udpBytesReply.length);

                udpClient.receive(reply);
                replyMessage = new String(reply.getData()).trim();

                this.logger.info(createLogMessage(
                        ParticipantRequestType.EXCHANGE_TICKETS.value,
                        parameters,
                        Message.FAILURE,
                        Message.CANCELLATION_FAILED_CLEANUP
                ));
                return new Response(Message.CANCELLATION_FAILED_CLEANUP, false);


            } catch (IOException e) {
                e.printStackTrace();
                return new Response("", false);
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }

        return new Response("", false);
    }

    protected String createLogMessage(String requestType, String parameters, String requestState, String response) {
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

    private static String getEventDate(String eventID) {
        System.out.println(eventID);
        return eventID.substring(4);
    }

    protected static int[] eventIdToWeek(String eventID) {
        System.out.println(eventID);
        String date = getEventDate(eventID);
        int day = Integer.parseInt(date.substring(0, 2));
        int month = Integer.parseInt(date.substring(2, 4));
        int year = 2000 + Integer.parseInt(date.substring(4));

        LocalDate localDate = LocalDate.of(year, month, day);

        return new int[]{localDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), localDate.get(IsoFields.WEEK_BASED_YEAR)};
    }

    public String getParticipantEvents(String participantID) {
        return eventRecords.values().stream().map(x -> x.entrySet().stream()
                .filter(entry -> entry.getValue().getParticipants().contains(participantID))
                .map(Map.Entry::getKey)
                .reduce("", this::eventsFormatter)
        )
            .filter(x -> x.length() > 0)
            .reduce("", this::eventsFormatter);
    }

    public String isEventAvailable(String eventType, String eventId) {
        return Boolean.toString(eventRecords.get(eventType)
                .getOrDefault(eventId, new Event(0, ""))
                .getCapacity() > 0);
    }

    private String eventsFormatter(String acc, String event) {
        if (acc.equals(""))
            return event;
        else
            return acc + "," + event;
    }
}
