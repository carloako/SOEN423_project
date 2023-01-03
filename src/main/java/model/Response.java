package model;

import java.io.Serializable;

public class Response implements Serializable {
    String message;
    boolean isSuccessful;

    public Response() {
        isSuccessful = false;
        message = "";
    }

    public Response(String message, boolean isSuccessful) {
        this.message = message;
        this.isSuccessful = isSuccessful;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSuccessful(boolean successful) {
        isSuccessful = successful;
    }
}
