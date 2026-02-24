package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IncrCommandHandler}.
 */
class IncrCommandHandlerTest {

    private InMemoryDataStore store;
    private IncrCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new IncrCommandHandler(store);
    }

    @Test
    void execute_initializesToOneWhenKeyDoesNotExist() {
        // Arrange — no key set

        // Act
        byte[] result = handler.execute(new String[]{"INCR", "counter"});

        // Assert
        assertEquals(":1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_incrementsExistingIntegerValue() {
        // Arrange
        store.setString("counter", "5", -1);

        // Act
        byte[] result = handler.execute(new String[]{"INCR", "counter"});

        // Assert
        assertEquals(":6\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_storesIncrementedValueInStore() {
        // Arrange
        store.setString("hits", "10", -1);

        // Act
        handler.execute(new String[]{"INCR", "hits"});

        // Assert
        assertEquals("11", store.getString("hits").get());
    }

    @Test
    void execute_returnsErrorWhenValueIsNotInteger() {
        // Arrange
        store.setString("name", "alice", -1);

        // Act
        byte[] result = handler.execute(new String[]{"INCR", "name"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("-"));
        assertTrue(encoded.contains("ERR"));
    }

    @Test
    void execute_handlesMultipleIncrements() {
        // Arrange — start at 0

        // Act
        handler.execute(new String[]{"INCR", "c"});
        handler.execute(new String[]{"INCR", "c"});
        byte[] result = handler.execute(new String[]{"INCR", "c"});

        // Assert — third INCR returns 3
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_preservesExpiryOnIncrement() {
        // Arrange — set a key with a future expiry
        long futureExpiry = System.currentTimeMillis() + 60_000;
        store.setString("timed", "7", futureExpiry);

        // Act
        handler.execute(new String[]{"INCR", "timed"});

        // Assert — expiry was preserved
        assertEquals(futureExpiry, store.getExpiry("timed"));
    }

    @Test
    void execute_incrementsNegativeValue() {
        // Arrange
        store.setString("neg", "-3", -1);

        // Act
        byte[] result = handler.execute(new String[]{"INCR", "neg"});

        // Assert
        assertEquals(":-2\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
