package com.ticketwaitingroom.common.exceptions;

public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(String message) {
        super(message);
    }
}
