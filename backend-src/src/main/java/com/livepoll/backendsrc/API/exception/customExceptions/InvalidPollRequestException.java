package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class InvalidPollRequestException extends CustomBaseException {

    public InvalidPollRequestException(String message, String errorCode) {
        super(message, errorCode);
    }

    public InvalidPollRequestException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
