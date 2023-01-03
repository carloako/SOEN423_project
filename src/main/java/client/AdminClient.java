package client;

import frontend.FrontEnd;
import model.Response;
import replicas.philippe.model.EventType;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class AdminClient extends ParticipantClient {

    private enum AdminRequestType {
        ADD_RESERVATION_SLOT("add reservation slot"),
        REMOVE_RESERVATION_SLOT("remove reservation slot"),
        LIST_AVAILABLE_SLOT("list available reservation slot");

        public final String value;

        AdminRequestType(String value) {
            this.value = value;
        }
    }

    public static void main(String[] args) {
        objectPattern = "Admin-%s";
        ipAddress = args[0];
        String adminID = args[1];
        logFileName = String.format("AdminLogs-%s.log", adminID);
        initializeLogger(AdminClient.class.getName());

        if (!isValidAdmin(adminID)) {
            System.out.println("You are not an admin.");
            System.out.println("Terminating server...");
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);
        int choice;

        System.out.println("Welcome participant");

        do {
            System.out.println("What do you want to do?");
            System.out.println("\t1. Add event reservation slot.");
            System.out.println("\t2. Remove event reservation slot.");
            System.out.println("\t3. List reservation slot available.");
            System.out.println("\t4. Reserve an event ticket.");
            System.out.println("\t5. Get my event schedule.");
            System.out.println("\t6. Cancel an event ticket");
            System.out.println("\t7. Exchange an event ticket");
            System.out.println("\t8. Quit");
            System.out.print("Please enter your choice: ");

            do {
                choice = scanner.nextInt();
            } while (invalidChoice(choice, 1, 8));

            switch (choice) {
                case 1:
                    addReservationSlot(scanner, adminID);
                    break;
                case 2:
                    removeReservationSlot(scanner, adminID);
                    break;
                case 3:
                    listAvailableReservationSlot(scanner, adminID);
                    break;
                case 4:
                    reserveTicket(scanner, adminID);
                    break;
                case 5:
                    getEventSchedule(scanner, adminID);
                    break;
                case 6:
                    cancelTicket(scanner, adminID);
                    break;
                case 7:
                    exchangeTicket(scanner, adminID);
                    break;
            }
        } while(choice != 8);

        System.out.println("Thanks for using our services.");
        System.out.println("Closing the program...");
        scanner.close();
    }

    public static FrontEnd createAdmin(String city) throws MalformedURLException {
        URL url = new URL(String.format("http://%s:9001/frontend-%s?wsdl", ipAddress, city));
        QName qName = new QName("http://frontend/", "FrontEndImplService");
        Service service = Service.create(url, qName);
        return service.getPort(FrontEnd.class);
    }

    static void addReservationSlot(Scanner scanner, String adminID) {
        String eventID;
        String eventType = "";
        int capacity;

        int choice;
        System.out.println("What event would you like to create?");
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

        System.out.println("How would you uniquely identify this event?");
        System.out.print("Please enter the event ID: ");
        eventID = scanner.next();

        System.out.println("What is the capacity of the event?");
        System.out.print("Please enter the capacity: ");
        capacity = scanner.nextInt();

        String city = eventID.substring(0, 3);

        try {
            FrontEnd admin = createAdmin(city);
            Response response = admin.addReservationSlot(eventID, eventType, capacity);

            logger.info(createLogMessage(
                    AdminRequestType.ADD_RESERVATION_SLOT.value,
                    String.format("%s,%s,%s", eventID, eventType, capacity),
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void removeReservationSlot(Scanner scanner, String adminID) {
        String eventID;
        String eventType = "";

        int choice;
        System.out.println("What event type would you like to cancel?");
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

        System.out.println("Which event would you like to cancel?");
        System.out.print("Please enter the event ID: ");
        eventID = scanner.next();

        String city = eventID.substring(0, 3);

        try {
            FrontEnd admin = createAdmin(city);
            Response response = admin.removeReservationSlot(eventID, eventType);

            logger.info(createLogMessage(
                    AdminRequestType.REMOVE_RESERVATION_SLOT.value,
                    String.format("%s,%s", eventID, eventType),
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void listAvailableReservationSlot(Scanner scanner, String adminID) {
        String eventType = "";

        int choice;
        System.out.println("What event type would you like see?");
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

        String city = adminID.substring(0, 3);

        try {
            FrontEnd admin = createAdmin(city);
            Response response = admin.listReservationSlotAvailable(eventType);

            logger.info(createLogMessage(
                    AdminRequestType.LIST_AVAILABLE_SLOT.value,
                    eventType,
                    response
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidAdmin(String adminID) {
        return adminID.charAt(3) == 'A';
    }
}
