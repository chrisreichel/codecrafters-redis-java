package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GetCommandHandler}.
 */
class GetCommandHandlerTest {

    private InMemoryDataStore store;
    private GetCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new GetCommandHandler(store);
    }

    @Test
    void execute_returnsValueAsBulkString_whenKeyExists() {
        // Arrange
        store.setString("mykey", "myvalue", -1);

        // Act
        byte[] result = handler.execute(new String[]{"GET", "mykey"});

        // Assert
        assertEquals("$7\r\nmyvalue\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsNullBulkString_whenKeyMissing() {
        // Arrange — key never set

        // Act
        byte[] result = handler.execute(new String[]{"GET", "nonexistent"});

        // Assert
        assertEquals("$-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsNullBulkString_whenKeyExpired() throws InterruptedException {
        // Arrange — key expires in 50 ms
        store.setString("expiring", "val", System.currentTimeMillis() + 50);

        // Let the key expire
        Thread.sleep(100);

        // Act
        byte[] result = handler.execute(new String[]{"GET", "expiring"});

        // Assert
        assertEquals("$-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsValue_afterSetWithNoExpiry() {
        // Arrange
        store.setString("persistent", "data", -1);

        // Act
        byte[] result = handler.execute(new String[]{"GET", "persistent"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("data"));
    }

    @Test
    void execute_returnsLatestValue_afterKeyOverwritten() {
        // Arrange
        store.setString("k", "original", -1);
        store.setString("k", "updated", -1);

        // Act
        byte[] result = handler.execute(new String[]{"GET", "k"});

        // Assert
        assertEquals("$7\r\nupdated\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
