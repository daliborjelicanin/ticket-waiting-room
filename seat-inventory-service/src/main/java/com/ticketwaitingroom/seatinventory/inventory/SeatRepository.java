package com.ticketwaitingroom.seatinventory.inventory;

import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.Map;

/**
 * Single-table DynamoDB access for seat inventory.
 *
 * <p>The whole point of this class is {@link #holdSeat}: acquiring a hold is an atomic,
 * race-free operation performed by a single {@code UpdateItem} guarded by a
 * {@code ConditionExpression}. DynamoDB serializes conditional writes per item, so
 * exactly one of many concurrent hold attempts for the same seat can win — with no
 * application-level lock and no read-modify-write window.
 *
 * <p>Item shape (single-table design, room to grow for holds/orders later):
 * <pre>
 *   PK  = EVENT#&lt;eventId&gt;
 *   SK  = SEAT#&lt;seatId&gt;
 *   status        = AVAILABLE | HELD | SOLD
 *   holdId        = current hold id      (absent when AVAILABLE)
 *   holdExpiresAt = epoch-seconds TTL    (absent when AVAILABLE)
 * </pre>
 */
@RequiredArgsConstructor
public class SeatRepository {

    static final String ATTR_PK = "pk";
    static final String ATTR_SK = "sk";
    static final String ATTR_EVENT_ID = "eventId";
    static final String ATTR_SEAT_ID = "seatId";
    static final String ATTR_STATUS = "status";
    static final String ATTR_HOLD_ID = "holdId";
    static final String ATTR_HOLD_EXPIRES_AT = "holdExpiresAt";

    private final DynamoDbClient dynamo;
    private final String tableName;

    /**
     * Seed a seat in the {@link SeatStatus#AVAILABLE} state. Fails if the seat already
     * exists, so seeding can never silently clobber a held/sold seat.
     */
    public void createAvailableSeat(String eventId, String seatId) {
        dynamo.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        ATTR_PK, s(pk(eventId)),
                        ATTR_SK, s(sk(seatId)),
                        ATTR_EVENT_ID, s(eventId),
                        ATTR_SEAT_ID, s(seatId),
                        ATTR_STATUS, s(SeatStatus.AVAILABLE.name())))
                .conditionExpression("attribute_not_exists(" + ATTR_PK + ")")
                .build());
    }

    /**
     * Atomically transition a seat from {@link SeatStatus#AVAILABLE} to
     * {@link SeatStatus#HELD}. Race-free: the {@code ConditionExpression} ensures to
     * write only applies if the seat is currently AVAILABLE, so concurrent callers for
     * the same seat cannot both succeed.
     *
     * @throws SeatUnavailableException if the seat is not AVAILABLE (already held, sold,
     *                                  or nonexistent) — i.e. this caller lost the race.
     */
    public void holdSeat(String eventId, String seatId, String holdId, Instant holdExpiresAt) {
        try {
            dynamo.updateItem(UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of(
                            ATTR_PK, s(pk(eventId)),
                            ATTR_SK, s(sk(seatId))))
                    .updateExpression("SET #status = :held, #holdId = :holdId, #ttl = :ttl")
                    .conditionExpression("#status = :available")
                    .expressionAttributeNames(Map.of(
                            "#status", ATTR_STATUS,
                            "#holdId", ATTR_HOLD_ID,
                            "#ttl", ATTR_HOLD_EXPIRES_AT))
                    .expressionAttributeValues(Map.of(
                            ":held", s(SeatStatus.HELD.name()),
                            ":available", s(SeatStatus.AVAILABLE.name()),
                            ":holdId", s(holdId),
                            ":ttl", n(holdExpiresAt.getEpochSecond())))
                    .build());
        } catch (ConditionalCheckFailedException e) {
            throw new SeatUnavailableException(
                    "Seat " + seatId + " of event " + eventId + " is not available to hold");
        }
    }

    /** Read a seat, or {@code null} if it does not exist. */
    public Seat getSeat(String eventId, String seatId) {
        Map<String, AttributeValue> item = dynamo.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, s(pk(eventId)),
                        ATTR_SK, s(sk(seatId))))
                .consistentRead(true)
                .build())
                .item();

        if (item == null || item.isEmpty()) {
            return null;
        }
        AttributeValue ttl = item.get(ATTR_HOLD_EXPIRES_AT);
        return new Seat(
                item.get(ATTR_EVENT_ID).s(),
                item.get(ATTR_SEAT_ID).s(),
                SeatStatus.valueOf(item.get(ATTR_STATUS).s()),
                item.containsKey(ATTR_HOLD_ID) ? item.get(ATTR_HOLD_ID).s() : null,
                ttl != null ? Instant.ofEpochSecond(Long.parseLong(ttl.n())) : null);
    }

    static String pk(String eventId) {
        return "EVENT#" + eventId;
    }

    static String sk(String seatId) {
        return "SEAT#" + seatId;
    }

    private static AttributeValue s(String value) {
        return AttributeValue.builder().s(value).build();
    }

    private static AttributeValue n(long value) {
        return AttributeValue.builder().n(Long.toString(value)).build();
    }
}
