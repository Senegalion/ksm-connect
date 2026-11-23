package com.ksmconnect.notification;

public class DeliveryException extends RuntimeException {

    private final boolean permanent;
    private final String errorCode;

    public DeliveryException(String errorCode, boolean permanent, Throwable cause) {
        super(errorCode, cause);
        this.errorCode = errorCode;
        this.permanent = permanent;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
