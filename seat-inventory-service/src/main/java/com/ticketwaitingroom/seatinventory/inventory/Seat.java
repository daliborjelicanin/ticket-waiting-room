package com.ticketwaitingroom.seatinventory.inventory;

import java.time.Instant;

/**
 * A seat within an event, as persisted in the single-table inventory store.
 *
 * <p>{@code holdExpiresAt} is populated only while the seat is {@link SeatStatus#HELD};
 * it is wired as the table's TTL attribute so holds can self-expire, and is removed on
 * purchase so a {@link SeatStatus#SOLD} seat can never be TTL-deleted. {@code holdId}
 * is present while HELD and kept on a SOLD seat as the audit trail of the winning hold;
 * both are {@code null} for an {@link SeatStatus#AVAILABLE} seat.
 */
public record Seat(
        String eventId,
        String seatId,
        SeatStatus status,
        String holdId,
        Instant holdExpiresAt
) {

    public static Seat available(String eventId, String seatId) {
        return new Seat(eventId, seatId, SeatStatus.AVAILABLE, null, null);
    }
}
