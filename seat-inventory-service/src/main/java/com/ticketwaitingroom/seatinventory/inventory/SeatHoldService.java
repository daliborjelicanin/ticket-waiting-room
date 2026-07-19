package com.ticketwaitingroom.seatinventory.inventory;

import com.ticketwaitingroom.common.exceptions.HoldNotFoundException;
import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import com.ticketwaitingroom.seatinventory.config.SeatInventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the seat-hold lifecycle: acquire, release, confirm. Generates
 * hold identity/expiry and supplies the current time; every atomic transition is
 * delegated to the {@link SeatRepository} conditional writes. The concurrency guarantee
 * lives entirely there — this layer adds no locking of its own.
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

    /**
     * Release a hold, making the seat available again.
     *
     * @throws HoldNotFoundException if the seat is not currently held under this holdId
     */
    public void releaseHold(String eventId, String seatId, String holdId) {
        seatRepository.releaseHold(eventId, seatId, holdId);
    }

    /**
     * Confirm the purchase of a held seat, marking it sold.
     *
     * @throws HoldNotFoundException if the seat is not currently held under this holdId,
     *                               or the hold has expired
     */
    public void confirmPurchase(String eventId, String seatId, String holdId) {
        seatRepository.confirmPurchase(eventId, seatId, holdId, clock.instant());
    }
}
