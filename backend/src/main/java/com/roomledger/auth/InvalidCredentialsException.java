package com.roomledger.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class InvalidCredentialsException extends ErrorResponseException {

    public InvalidCredentialsException() {
        // Same message for "unknown email" and "wrong password", so attackers cannot find out which emails exist.
        super(HttpStatus.UNAUTHORIZED,
                ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password"), null);
    }
}
