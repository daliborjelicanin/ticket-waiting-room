package com.ticketwaitingroom.seatinventory.inventory;

import java.time.Instant;

/**
 * The result of successfully acquiring a hold on a seat.
 */
public record SeatHold(
        String eventId,
        String seatId,
        String holdId,
        Instant expiresAt
) {
}
