package com.roomledger.common.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.time.Duration;

public class TooManyRequestsException extends ErrorResponseException {

    public TooManyRequestsException(String detail, Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, detail), null);
        // Standard header telling the client how many seconds to wait before trying again.
        getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max(1, retryAfter.toSeconds())));
    }
}
