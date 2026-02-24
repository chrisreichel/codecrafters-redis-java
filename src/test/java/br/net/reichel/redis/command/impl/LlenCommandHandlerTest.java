package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LlenCommandHandler}.
 */
class LlenCommandHandlerTest {

    private InMemoryDataStore store;
    private LlenCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new LlenCommandHandler(store);
    }

    @Test
    void execute_returnsZeroForNonExistentKey() {
        // Arrange — key does not exist

        // Act
        byte[] result = handler.execute(new String[]{"LLEN", "ghost"});

        // Assert
        assertEquals(":0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsCorrectLengthAfterRpush() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act
        byte[] result = handler.execute(new String[]{"LLEN", "mylist"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsCorrectLengthAfterLpush() {
        // Arrange
        store.lpush("mylist", List.of("x", "y"));

        // Act
        byte[] result = handler.execute(new String[]{"LLEN", "mylist"});

        // Assert
        assertEquals(":2\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsUpdatedLengthAfterAdditionalPush() {
        // Arrange — list starts with one element
        store.rpush("mylist", List.of("first"));

        // Act — push two more
        store.rpush("mylist", List.of("second", "third"));
        byte[] result = handler.execute(new String[]{"LLEN", "mylist"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsDecreasedLengthAfterLpop() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));
        store.lpop("mylist");

        // Act
        byte[] result = handler.execute(new String[]{"LLEN", "mylist"});

        // Assert
        assertEquals(":2\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsZeroAfterAllElementsPopped() {
        // Arrange
        store.rpush("mylist", List.of("only"));
        store.lpop("mylist");

        // Act
        byte[] result = handler.execute(new String[]{"LLEN", "mylist"});

        // Assert — list still exists in store but is empty; llen should still return 0
        assertEquals(":0\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
