package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class ExpiredTokenException extends CustomBaseException {
    public ExpiredTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public ExpiredTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
