package com.roomledger.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** For business-rule violations that bean validation (@NotNull, @Size...) cannot express. */
public class BadRequestException extends ErrorResponseException {

    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail), null);
    }
}
