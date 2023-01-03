package replicas.philippe.Client;

import replicas.philippe.model.Response;
import replicas.philippe.model.EventType;
import replicas.philippe.model.Message;
import replicas.philippe.Server.Participant;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ParticipantClient {
    static String logFileName;
    static Logger logger;
    static String objectPattern;

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

    protected static Map<String, Integer> ports = new HashMap<String, Integer>() {{
        put("MTL", 16080);
        put("TOR", 16089);
        put("VAN", 16099);
    }};

    public static void main(String[] args) {
        objectPattern = "participant-%s";
        String participantID = args[0];
        System.out.println(participantID);
        logFileName = String.format("ParticipantLogs-%s.log", participantID);
        initializeLogger(ParticipantClient.class.getName());

        Scanner scanner = new Scanner(System.in);
        int choice;

        System.out.println("Welcome participant");

        do {
            System.out.println("What do you want to do?");
            System.out.println("\t1. Reserve an event ticket.");
            System.out.println("\t2. Get my event schedule.");
            System.out.println("\t3. Cancel an event ticket");
            System.out.println("\t4. Exchange my ticket");
            System.out.println("\t5. Quit");
            System.out.print("Please enter your choice: ");

            do {
                choice = scanner.nextInt();
            } while (invalidChoice(choice, 1, 5));

            switch (choice) {
                case 1:
                    reserveTicket(scanner, participantID);
                    break;
                case 2:
                    getEventSchedule(scanner, participantID);
                    break;
                case 3:
                    cancelTicket(scanner, participantID);
                    break;
                case 4:
                    exchangeTicket(scanner, participantID);
                    break;
            }
        } while(choice != 5);

        System.out.println("Thanks for using our services.");
        System.out.println("Closing the program...");
        scanner.close();

    }

    static void initializeLogger(String loggerName) {
        logger = Logger.getLogger(loggerName);
        try {
            FileHandler fileHandler = new FileHandler(logFileName);
            logger.addHandler(fileHandler);
            fileHandler.setFormatter(new SimpleFormatter());
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    static String createLogMessage(String requestType, String parameters, Response response) {
        Date date = new Date();
        String dateFormatted = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(date);
        return String.format(
                "%s;%s;%s;%s;%s",
                dateFormatted,
                requestType,
                parameters,
                response.isSuccessful() ? Message.SUCCESS : Message.FAILURE,
                response.getMessage()
                );
    }

    static boolean invalidChoice(int choice, int lowerBound, int upperBound) {
        if (choice < lowerBound || choice > upperBound) {
            System.out.println("Please enter a valid choice.");
            return true;
        }
        return false;
    }

    public static Participant createParticipant(String city) throws MalformedURLException {
        URL url = new URL(String.format("http://localhost:%d/participant-%s?wsdl", ports.get(city), city));
        QName qName = new QName("http://Server.philippe.replicas/", "ParticipantImplService");
        Service service = Service.create(url, qName);
        return service.getPort(Participant.class);
    }

    static void reserveTicket(Scanner scanner, String participantID) {
        String eventType = "";
        String eventID;

        int choice;
        System.out.println("What event would you like to attend?");
        System.out.println("\t1. Art Gallery");
        System.out.println("\t2. Concert");
        System.out.println("\t3. Theatre");
        System.out.print("Please enter your choice: ");

        do {
            choice = scanner.nextInt();
        } while (invalidChoice(choice, 1, 3));

        switch (choice) {
            case 1:
                eventType = EventType.ART_GALLERY.value;
                break;
            case 2:
                eventType = EventType.CONCERT.value;
                break;
            case 3:
                eventType = EventType.THEATRE.value;
                break;
        }

        System.out.println("Which event would you like to attend?");
        System.out.print("Please enter the event ID: ");
        eventID = scanner.next();

        String city = eventID.substring(0, 3);

        try {
            Participant participant = createParticipant(city);
            Response response = participant.reserveTicket(participantID, eventID, eventType);

            logger.info(createLogMessage(
                    ParticipantRequestType.RESERVE_TICKET.value,
                    String.format("%s,%s,%s", participantID, eventID, eventType),
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static void getEventSchedule(Scanner scanner, String participantID) {
        System.out.println("Searching for you event schedule....");

        String city = participantID.substring(0, 3);

        try {
            Participant participant = createParticipant(city);
            Response response = participant.getEventSchedule(participantID);

            logger.info(createLogMessage(
                    ParticipantRequestType.GET_SCHEDULE.value,
                    participantID,
                    response
            ));

            System.out.println(response.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void cancelTicket(Scanner scanner, String participantID) {
        String eventID;

        System.out.println("Which event would you like to cancel?");
        System.out.print("Please enter the event ID: ");
        eventID = scanner.next();

        String city = eventID.substring(0, 3);

        try {
            Participant participant = createParticipant(city);
            Response response = participant.cancelTicket(participantID, eventID);

            logger.info(createLogMessage(
                    ParticipantRequestType.CANCEL_TICKET.value,
                    String.format("%s,%s", participantID, eventID),
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void exchangeTicket(Scanner scanner, String participantID) {
        String eventID;
        String newEventID;
        String newEventType = "";

        System.out.println("Which event would you like to exchange?");
        System.out.print("Please enter the event ID: ");
        eventID = scanner.next();

        int choice;
        System.out.println("Which event type would you like to attend instead?");
        System.out.println("\t1. Art Gallery");
        System.out.println("\t2. Concert");
        System.out.println("\t3. Theatre");
        System.out.print("Please enter your choice: ");

        do {
            choice = scanner.nextInt();
        } while (invalidChoice(choice, 1, 3));

        switch (choice) {
            case 1:
                newEventType = EventType.ART_GALLERY.value;
                break;
            case 2:
                newEventType = EventType.CONCERT.value;
                break;
            case 3:
                newEventType = EventType.THEATRE.value;
                break;
        }

        System.out.println("Which event would you like to instead attend?");
        System.out.print("Please enter the event ID: ");
        newEventID = scanner.next();

        String city = eventID.substring(0, 3);

        try {
            Participant participant = createParticipant(city);
            Response response = participant.exchangeTickets(participantID, eventID, newEventID, newEventType);

            logger.info(createLogMessage(
                    ParticipantRequestType.EXCHANGE_TICKETS.value,
                    String.format("%s,%s,%s,%s", participantID, eventID, newEventID, newEventType),
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
