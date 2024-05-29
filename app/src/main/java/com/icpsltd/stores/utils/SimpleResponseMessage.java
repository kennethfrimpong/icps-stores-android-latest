package com.icpsltd.stores.utils;

public class SimpleResponseMessage {
    private String status;
    private String message;

    public SimpleResponseMessage() {
        }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
