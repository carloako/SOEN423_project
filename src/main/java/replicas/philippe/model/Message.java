package replicas.philippe.model;

public class Message {
    public static String SUCCESS = "success";
    public static String FAILURE = "failure";

    public static String RESERVE_TICKET_NO_CAPACITY = "No more available ticket";
    public static String RESERVE_TICKET_ALREADY_RESERVED = "Participant has already booked this event";
    public static String REACH_WEEKLY_RESERVE_LIMIT = "Participant has reach his 3 events per week limit";
    public static String REACH_DAILY_RESERVE_LIMIT = "Participant has reach his 1 event per day limit for a specific event type";
    public static String RESERVE_TICKET_SUCCESS = "Ticket reserved successfully";

    public static String EVENT_DOESNT_EXIST = "Event doesn't exist";
    public static String CANCEL_TICKET_NO_RESERVATION = "Participant doesn't have a reserved ticket for the event";
    public static String CANCEL_TICKET_SUCCESSFUL = "Ticket has been successfully cancel";

    public static String SCHEDULE_PROBLEM = "There was a problem getting the schedule";
    public static String LIST_PROBLEM = "There was a problem getting the schedule";

    public static String RESERVATION_SLOT_ALREADY_TAKEN = "Event slot is already used by another event";
    public static String RESERVATION_SLOT_SUCCESSFUL = "Event has been successfully added to the slot";

    public static String EVENT_ALREADY_RESERVED = "Participant has already reserved the event";
    public static String EVENT_REMOVED_SUCCESSFUL = "Event has been successfully removed";

    public static String NO_EVENT_RESERVATION = "Participant has no reservation for the event";
    public static String NEW_TICKET_NOT_AVAILABLE = "New event has no more tickets available";
    public static String NEW_TICKET_FAILED_RESERVATION = "The reservation for the new event failed.";
    public static String CANCELLATION_FAILED_CLEANUP = "The old event cancellation failed. The new event reservation was cancel.";
    public static String TICKET_EXCHANGE_SUCCESSFUL = "The ticket exchange was successful";
}
