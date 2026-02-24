package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RpushCommandHandler}.
 */
class RpushCommandHandlerTest {

    private InMemoryDataStore store;
    private RpushCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new RpushCommandHandler(store);
    }

    @Test
    void execute_returnsSizeOneWhenPushingSingleValueToNewKey() {
        // Arrange — no pre-existing list

        // Act
        byte[] result = handler.execute(new String[]{"RPUSH", "mylist", "a"});

        // Assert
        assertEquals(":1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsCorrectSizeWhenPushingMultipleValuesAtOnce() {
        // Arrange — no pre-existing list

        // Act
        byte[] result = handler.execute(new String[]{"RPUSH", "mylist", "a", "b", "c"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsAccumulatedSizeWhenAppendingToExistingList() {
        // Arrange — prime the list with two elements
        store.rpush("mylist", java.util.List.of("x", "y"));

        // Act
        byte[] result = handler.execute(new String[]{"RPUSH", "mylist", "z"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_appendsValuesToTailInOrder() {
        // Arrange
        handler.execute(new String[]{"RPUSH", "mylist", "first"});

        // Act
        handler.execute(new String[]{"RPUSH", "mylist", "second", "third"});

        // Assert — verify ordering via store
        java.util.List<String> all = store.lrange("mylist", 0, -1);
        assertEquals(java.util.List.of("first", "second", "third"), all);
    }

    @Test
    void execute_createsNewListWhenKeyDidNotExist() {
        // Arrange — key is absent

        // Act
        handler.execute(new String[]{"RPUSH", "newkey", "val"});

        // Assert
        assertEquals(1, store.llen("newkey"));
    }

    @Test
    void execute_incrementallySizeIsCorrectAfterRepeatedPushes() {
        // Act & Assert
        for (int i = 1; i <= 5; i++) {
            byte[] result = handler.execute(new String[]{"RPUSH", "k", "v" + i});
            assertEquals(":" + i + "\r\n", new String(result, StandardCharsets.UTF_8));
        }
    }
}
