package com.livepoll.backendsrc.API.exception;


import com.livepoll.backendsrc.API.exception.customExceptions.*;
import com.livepoll.backendsrc.API.records.UserRecords;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            InvalidInquiryException.class,
            ParseTokenException.class,
            InvalidRefreshTokenException.class,
            ExpiredTokenException.class,
            DuplicateUserException.class
    })
    public ResponseEntity<UserRecords.ApiResponse<UserRecords.ErrorResponseRecord>> handleException(CustomBaseException ex) {
        return ResponseEntity
                .badRequest()
                .body(
                        UserRecords.ApiResponse.error(
                                ex.getMessage(),
                                new UserRecords.ErrorResponseRecord(
                                    ex.getErrorCode()
                                )
                        )
                );
    }

}

