package com.ksmconnect.common.error;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.TOO_MANY_REQUESTS;
    }
}
