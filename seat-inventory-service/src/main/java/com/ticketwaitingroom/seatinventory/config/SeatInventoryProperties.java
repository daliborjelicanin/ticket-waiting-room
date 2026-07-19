package com.ticketwaitingroom.seatinventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Tunables for the seat-inventory service.
 *
 * @param tableName    the DynamoDB single-table name
 * @param holdDuration how long a seat hold is valid; also written as the item's TTL
 */
@ConfigurationProperties(prefix = "seat-inventory")
public record SeatInventoryProperties(
        String tableName,
        Duration holdDuration
) {
}
