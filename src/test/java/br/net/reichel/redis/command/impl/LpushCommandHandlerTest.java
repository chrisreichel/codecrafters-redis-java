package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LpushCommandHandler}.
 */
class LpushCommandHandlerTest {

    private InMemoryDataStore store;
    private LpushCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new LpushCommandHandler(store);
    }

    @Test
    void execute_returnsSizeOneWhenPushingSingleValueToNewKey() {
        // Arrange — no pre-existing list

        // Act
        byte[] result = handler.execute(new String[]{"LPUSH", "mylist", "a"});

        // Assert
        assertEquals(":1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsCorrectSizeWhenPushingMultipleValuesAtOnce() {
        // Arrange — no pre-existing list

        // Act
        byte[] result = handler.execute(new String[]{"LPUSH", "mylist", "a", "b", "c"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsAccumulatedSizeWhenPrependingToExistingList() {
        // Arrange — prime the list with two elements at tail
        store.rpush("mylist", List.of("x", "y"));

        // Act
        byte[] result = handler.execute(new String[]{"LPUSH", "mylist", "z"});

        // Assert
        assertEquals(":3\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_prependsEachValueToHeadSoLastArgBecomesHead() {
        // Arrange — LPUSH a b c means each value is inserted at head:
        // after "a" -> [a], after "b" -> [b, a], after "c" -> [c, b, a]

        // Act
        handler.execute(new String[]{"LPUSH", "mylist", "a", "b", "c"});

        // Assert — the store's lpush inserts each in order at position 0
        List<String> all = store.lrange("mylist", 0, -1);
        assertEquals("c", all.get(0));
        assertEquals("b", all.get(1));
        assertEquals("a", all.get(2));
    }

    @Test
    void execute_createsNewListWhenKeyDidNotExist() {
        // Arrange — key is absent

        // Act
        handler.execute(new String[]{"LPUSH", "brand-new", "val"});

        // Assert
        assertEquals(1, store.llen("brand-new"));
    }

    @Test
    void execute_incrementallySizeIsCorrectAfterRepeatedPushes() {
        // Act & Assert — each individual LPUSH of one element increases size by one
        for (int i = 1; i <= 4; i++) {
            byte[] result = handler.execute(new String[]{"LPUSH", "k", "v" + i});
            assertEquals(":" + i + "\r\n", new String(result, StandardCharsets.UTF_8));
        }
    }
}
