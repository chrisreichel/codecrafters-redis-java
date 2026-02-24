package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SetCommandHandler}.
 */
class SetCommandHandlerTest {

    private InMemoryDataStore store;
    private SetCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new SetCommandHandler(store);
    }

    @Test
    void execute_returnsOkSimpleString() {
        // Arrange
        String[] args = {"SET", "mykey", "myvalue"};

        // Act
        byte[] result = handler.execute(args);

        // Assert
        assertEquals("+OK\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_storesValueInDataStore() {
        // Arrange
        String[] args = {"SET", "greeting", "hello"};

        // Act
        handler.execute(args);

        // Assert
        assertTrue(store.getString("greeting").isPresent());
        assertEquals("hello", store.getString("greeting").get());
    }

    @Test
    void execute_withPxOption_storesWithExpiry() throws InterruptedException {
        // Arrange — 500 ms TTL
        String[] args = {"SET", "tempkey", "tempval", "PX", "500"};

        // Act
        handler.execute(args);

        // Assert — key is present immediately
        assertTrue(store.getString("tempkey").isPresent());

        // Assert — key has a positive expiry timestamp
        long expiry = store.getExpiry("tempkey");
        assertTrue(expiry > System.currentTimeMillis());
    }

    @Test
    void execute_withPxOption_keyExpiresAfterTtl() throws InterruptedException {
        // Arrange — very short TTL of 100 ms
        String[] args = {"SET", "shortlived", "value", "PX", "100"};

        // Act
        handler.execute(args);

        // Assert — key still present right away
        assertTrue(store.getString("shortlived").isPresent());

        // Wait for expiry
        Thread.sleep(150);

        // Assert — key has expired
        assertFalse(store.getString("shortlived").isPresent());
    }

    @Test
    void execute_withExOption_storesWithSecondsExpiry() {
        // Arrange — 10 second TTL
        String[] args = {"SET", "exkey", "exval", "EX", "10"};

        // Act
        handler.execute(args);

        // Assert — key is present
        assertTrue(store.getString("exkey").isPresent());

        // Assert — expiry is roughly 10 seconds in the future
        long expiry = store.getExpiry("exkey");
        long now = System.currentTimeMillis();
        assertTrue(expiry > now + 9_000);
        assertTrue(expiry < now + 11_000);
    }

    @Test
    void execute_withoutExpiry_storesWithNoExpiry() {
        // Arrange
        String[] args = {"SET", "persistent", "value"};

        // Act
        handler.execute(args);

        // Assert — expiry is -1 (no expiry)
        assertEquals(-1L, store.getExpiry("persistent"));
    }

    @Test
    void execute_overwritesExistingKey() {
        // Arrange
        handler.execute(new String[]{"SET", "key", "first"});

        // Act
        handler.execute(new String[]{"SET", "key", "second"});

        // Assert
        assertEquals("second", store.getString("key").get());
    }
}
