package com.ticketwaitingroom.seatinventory.inventory;

import java.time.Instant;

/**
 * A seat within an event, as persisted in the single-table inventory store.
 *
 * <p>{@code holdId} and {@code holdExpiresAt} are populated only while the seat is
 * {@link SeatStatus#HELD}; they are {@code null} for an {@link SeatStatus#AVAILABLE}
 * seat. {@code holdExpiresAt} is wired as the table's TTL attribute so holds can
 * self-expire — the reclaim-on-read logic is a later slice.
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
