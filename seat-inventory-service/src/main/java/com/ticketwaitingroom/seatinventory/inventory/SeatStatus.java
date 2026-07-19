package com.ticketwaitingroom.seatinventory.inventory;

/**
 * Lifecycle of a single seat. A seat may only be held while {@link #AVAILABLE};
 * the transition to {@link #HELD} is guarded by a DynamoDB conditional write so it
 * is race-free under concurrent hold attempts for the same seat.
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    SOLD
}
