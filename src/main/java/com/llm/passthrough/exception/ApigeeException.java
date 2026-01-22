package com.llm.passthrough.exception;

import lombok.Getter;

@Getter
public class ApigeeException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public ApigeeException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public ApigeeException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.responseBody = null;
    }
}
