package replicas.philippe.model;

public enum EventType {
    ART_GALLERY("art"),
    THEATRE("theatre"),
    CONCERT("concert");

    public final String value;

    EventType(String value) {
        this.value = value;
    }
}
