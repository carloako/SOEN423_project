package replicas.philippe.model;

import java.io.Serializable;

public class Response implements Serializable {
    private boolean isSuccessful;
    private String message;

    public Response() {
        isSuccessful = false;
        message = "";
    }

    public Response(String message, boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
        this.message = message;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getMessage() {
        return message;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
