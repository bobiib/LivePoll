package com.livepoll.backendsrc.API.exception.customExceptions;

import com.livepoll.backendsrc.API.exception.CustomBaseException;

public class InvalidInquiryException extends CustomBaseException {
    public InvalidInquiryException(String message, String errorCode) {
        super(message, errorCode);
    }
    public InvalidInquiryException(String message, String errorCode, Exception e) {
        super(message, errorCode, e);
    }
}