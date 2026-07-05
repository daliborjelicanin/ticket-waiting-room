package com.ticketwaitingroom.common.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SeatHoldExpiredEventTest {

    @Test
    void retainsAllFields() {
        Instant now = Instant.now();
        var event = new SeatHoldExpiredEvent("evt-1", "seat-1", "hold-1", now);

        assertThat(event.eventId()).isEqualTo("evt-1");
        assertThat(event.seatId()).isEqualTo("seat-1");
        assertThat(event.holdId()).isEqualTo("hold-1");
        assertThat(event.expiredAt()).isEqualTo(now);
    }
}
