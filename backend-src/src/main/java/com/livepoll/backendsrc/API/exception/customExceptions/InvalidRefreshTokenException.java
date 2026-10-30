package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class InvalidRefreshTokenException extends CustomBaseException {
    public InvalidRefreshTokenException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidRefreshTokenException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
