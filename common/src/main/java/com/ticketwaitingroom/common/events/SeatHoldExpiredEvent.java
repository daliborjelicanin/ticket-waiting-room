package com.ticketwaitingroom.common.events;

import java.time.Instant;

public record SeatHoldExpiredEvent(String eventId, String seatId, String holdId, Instant expiredAt) {
}
