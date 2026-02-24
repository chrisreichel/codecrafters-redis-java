package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeCommandHandler}.
 */
class TypeCommandHandlerTest {

    private InMemoryDataStore store;
    private TypeCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new TypeCommandHandler(store);
    }

    @Test
    void execute_returnsNone_whenKeyDoesNotExist() {
        // Arrange — key never created

        // Act
        byte[] result = handler.execute(new String[]{"TYPE", "ghost"});

        // Assert
        assertEquals("+none\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsString_whenKeyHoldsStringValue() {
        // Arrange
        store.setString("greeting", "hello", -1);

        // Act
        byte[] result = handler.execute(new String[]{"TYPE", "greeting"});

        // Assert
        assertEquals("+string\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsList_whenKeyHoldsListValue() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act
        byte[] result = handler.execute(new String[]{"TYPE", "mylist"});

        // Assert
        assertEquals("+list\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsStream_whenKeyHoldsStreamValue() {
        // Arrange
        store.appendStreamEntry("mystream", "1000-0", List.of("field", "value"));

        // Act
        byte[] result = handler.execute(new String[]{"TYPE", "mystream"});

        // Assert
        assertEquals("+stream\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsString_evenForExpiredKey() throws InterruptedException {
        // Arrange — the DataStore.type() implementation checks containsKey() on the raw map,
        // so it does not apply TTL filtering; an expired string key still reports "string".
        store.setString("expiring", "val", System.currentTimeMillis() + 50);
        Thread.sleep(100);

        // Act
        byte[] result = handler.execute(new String[]{"TYPE", "expiring"});

        // Assert — type() sees the raw entry and returns "string", even though getString() returns empty
        assertEquals("+string\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
