package com.ticketwaitingroom.seatinventory.web;

import com.ticketwaitingroom.seatinventory.inventory.SeatHold;
import com.ticketwaitingroom.seatinventory.inventory.SeatHoldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * HTTP surface for seat holds. A successful hold returns 201; an unavailable seat is
 * mapped to 409 by {@link ApiExceptionHandler}.
 */
@RestController
@RequiredArgsConstructor
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    @PostMapping("/events/{eventId}/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.CREATED)
    public SeatHoldResponse hold(@PathVariable String eventId, @PathVariable String seatId) {
        SeatHold hold = seatHoldService.holdSeat(eventId, seatId);
        return new SeatHoldResponse(hold.eventId(), hold.seatId(), hold.holdId(), hold.expiresAt());
    }

    public record SeatHoldResponse(
            String eventId,
            String seatId,
            String holdId,
            Instant expiresAt
    ) {
    }
}
