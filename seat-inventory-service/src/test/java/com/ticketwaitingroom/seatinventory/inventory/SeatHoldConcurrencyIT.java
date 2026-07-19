package com.ticketwaitingroom.seatinventory.inventory;

import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the project's centerpiece: atomic, race-free seat locking.
 *
 * <p>Runs against a real DynamoDB (LocalStack in Docker) so the concurrency guarantee is
 * exercised end-to-end, not mocked. Many virtual threads race to hold the <em>same</em>
 * AVAILABLE seat; the DynamoDB {@code ConditionExpression} in {@link SeatRepository#holdSeat}
 * must let exactly one win.
 */
@Testcontainers
class SeatHoldConcurrencyIT {

    private static final String TABLE = "seat-inventory";
    private static final String EVENT_ID = "evt-1";
    private static final String SEAT_ID = "A-12";

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"));

    private static DynamoDbClient dynamo;
    private SeatRepository repository;

    @BeforeAll
    static void createClient() {
        dynamo = DynamoDbClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
    }

    @AfterAll
    static void closeClient() {
        if (dynamo != null) {
            dynamo.close();
        }
    }

    @BeforeEach
    void freshTableWithOneAvailableSeat() {
        recreateTable();
        repository = new SeatRepository(dynamo, TABLE);
        repository.createAvailableSeat(EVENT_ID, SEAT_ID);
    }

    @Test
    void exactlyOneConcurrentHoldWins() throws InterruptedException {
        int attempts = 100;
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicReference<String> winningHoldId = new AtomicReference<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < attempts; i++) {
                String holdId = "hold-" + i;
                executor.submit(() -> {
                    try {
                        startGate.await(); // release all threads at once for maximum contention
                        repository.holdSeat(EVENT_ID, SEAT_ID, holdId, expiresAt);
                        successes.incrementAndGet();
                        winningHoldId.set(holdId);
                    } catch (SeatUnavailableException lost) {
                        failures.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            startGate.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).as("all hold attempts finished").isTrue();
        }

        assertThat(successes).as("exactly one hold succeeds").hasValue(1);
        assertThat(failures).as("every other attempt is rejected").hasValue(attempts - 1);

        Seat seat = repository.getSeat(EVENT_ID, SEAT_ID);
        assertThat(seat).isNotNull();
        assertThat(seat.status()).isEqualTo(SeatStatus.HELD);
        assertThat(seat.holdId()).isEqualTo(winningHoldId.get());
    }

    private void recreateTable() {
        try {
            dynamo.deleteTable(DeleteTableRequest.builder().tableName(TABLE).build());
            dynamo.waiter().waitUntilTableNotExists(b -> b.tableName(TABLE));
        } catch (ResourceNotFoundException ignored) {
            // first run: nothing to delete
        }

        dynamo.createTable(CreateTableRequest.builder()
                .tableName(TABLE)
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName(SeatRepository.ATTR_PK)
                                .attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName(SeatRepository.ATTR_SK)
                                .attributeType(ScalarAttributeType.S).build())
                .keySchema(
                        KeySchemaElement.builder().attributeName(SeatRepository.ATTR_PK)
                                .keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName(SeatRepository.ATTR_SK)
                                .keyType(KeyType.RANGE).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());
        dynamo.waiter().waitUntilTableExists(b -> b.tableName(TABLE));

        dynamo.updateTimeToLive(UpdateTimeToLiveRequest.builder()
                .tableName(TABLE)
                .timeToLiveSpecification(TimeToLiveSpecification.builder()
                        .enabled(true)
                        .attributeName(SeatRepository.ATTR_HOLD_EXPIRES_AT)
                        .build())
                .build());
    }
}
