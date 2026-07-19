package com.ticketwaitingroom.seatinventory.inventory;

/**
 * Lifecycle of a single seat: {@link #AVAILABLE} → {@link #HELD} → {@link #SOLD},
 * with release/expiry taking a HELD seat back to AVAILABLE. Every transition is
 * guarded by a DynamoDB conditional write so it is race-free under concurrent
 * attempts for the same seat.
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    SOLD
}
