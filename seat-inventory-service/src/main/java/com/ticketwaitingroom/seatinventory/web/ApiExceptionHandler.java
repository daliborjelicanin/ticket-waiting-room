package com.ticketwaitingroom.seatinventory.web;

import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain exceptions to HTTP responses. Losing the race for a seat is an expected,
 * benign outcome for a client, so it becomes a 409 Conflict rather than a 5xx.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SeatUnavailableException.class)
    public ProblemDetail handleSeatUnavailable(SeatUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Seat unavailable");
        return problem;
    }
}
