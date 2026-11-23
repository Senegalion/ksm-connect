package com.ksmconnect.common.error;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
