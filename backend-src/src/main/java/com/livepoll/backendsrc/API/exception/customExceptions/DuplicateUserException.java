package com.livepoll.backendsrc.API.exception.customExceptions;


import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class DuplicateUserException extends CustomBaseException {
    public DuplicateUserException(String message, String errorCode) {
        super(message, errorCode);
    }
    public DuplicateUserException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
