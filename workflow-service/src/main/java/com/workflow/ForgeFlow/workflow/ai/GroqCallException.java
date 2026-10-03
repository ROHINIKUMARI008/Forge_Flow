package com.workflow.ForgeFlow.workflow.ai;

import org.springframework.http.HttpStatus;

public class GroqCallException extends RuntimeException {

    private final HttpStatus status;

    public GroqCallException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
