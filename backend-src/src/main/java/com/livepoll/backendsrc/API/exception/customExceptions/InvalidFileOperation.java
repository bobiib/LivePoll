package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class InvalidFileOperation extends CustomBaseException {
    public InvalidFileOperation(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidFileOperation(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}
