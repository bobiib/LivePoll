package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class ParseTokenException extends CustomBaseException {
    public ParseTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ParseTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
