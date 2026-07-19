package com.ticketwaitingroom.seatinventory.inventory;

import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import com.ticketwaitingroom.seatinventory.config.SeatInventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service for holding seats. Generates the hold identity and expiry, then
 * delegates the atomic acquisition to {@link SeatRepository#holdSeat}. The concurrency
 * guarantee lives entirely in the repository's conditional write — this layer adds no
 * locking of its own.
 */
@Service
@RequiredArgsConstructor
public class SeatHoldService {

    private final SeatRepository seatRepository;
    private final SeatInventoryProperties properties;
    private final Clock clock;

    /**
     * Attempt to hold a seat for the configured duration.
     *
     * @throws SeatUnavailableException if the seat is not available (lost the race, or
     *                                  already held/sold)
     */
    public SeatHold holdSeat(String eventId, String seatId) {
        String holdId = UUID.randomUUID().toString();
        Instant expiresAt = clock.instant().plus(properties.holdDuration());
        seatRepository.holdSeat(eventId, seatId, holdId, expiresAt);
        return new SeatHold(eventId, seatId, holdId, expiresAt);
    }
}
