package replicas.philippe.model;

import java.util.ArrayList;
import java.util.List;

public class Event {
    private int capacity;
    private String eventType;
    private List<String> participants;

    public Event(int capacity, String eventType) {
        this.capacity = capacity;
        this.eventType = eventType;
        this.participants = new ArrayList<>();
    }

    public int getCapacity() {
        return capacity;
    }

    public void increaseCapacity() {
        this.capacity += 1;
    }

    public void decreaseCapacity() {
        this.capacity -= 1;
    }

    public String getEventType() {
        return eventType;
    }

    public void removeParticipant(String participantID) {
        this.participants.remove(participantID);
        increaseCapacity();
    }

    public void addParticipant(String participantID) {
        this.participants.add(participantID);
        decreaseCapacity();
    }

    public List<String> getParticipants() {
        return participants;
    }
}
