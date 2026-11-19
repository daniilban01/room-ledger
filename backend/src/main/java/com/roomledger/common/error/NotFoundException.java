package com.roomledger.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * Extending ErrorResponseException lets Spring render the exception as an RFC 7807 ProblemDetail
 * without a dedicated handler method for every exception type.
 */
public class NotFoundException extends ErrorResponseException {

    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, detail), null);
    }
}
