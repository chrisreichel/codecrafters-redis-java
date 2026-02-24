package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LpopCommandHandler}.
 */
class LpopCommandHandlerTest {

    private InMemoryDataStore store;
    private LpopCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new LpopCommandHandler(store);
    }

    // -------------------------------------------------------------------------
    // Single-pop variant (no count argument)
    // -------------------------------------------------------------------------

    @Test
    void execute_returnsBulkStringWithHeadElement() {
        // Arrange
        store.rpush("mylist", List.of("first", "second"));

        // Act
        byte[] result = handler.execute(new String[]{"LPOP", "mylist"});

        // Assert
        assertEquals("$5\r\nfirst\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_removesHeadElementFromStore() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act
        handler.execute(new String[]{"LPOP", "mylist"});

        // Assert — "a" was removed; list now starts with "b"
        assertEquals("b", store.lpop("mylist").get());
        assertEquals(1, store.llen("mylist"));
    }

    @Test
    void execute_returnsNullBulkStringForNonExistentKey() {
        // Arrange — key does not exist

        // Act
        byte[] result = handler.execute(new String[]{"LPOP", "ghost"});

        // Assert
        assertEquals("$-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Count variant (with count argument)
    // -------------------------------------------------------------------------

    @Test
    void execute_returnsArrayOfNElementsWhenCountProvided() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c", "d"));

        // Act
        byte[] result = handler.execute(new String[]{"LPOP", "mylist", "2"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertTrue(encoded.contains("$1\r\na\r\n"));
        assertTrue(encoded.contains("$1\r\nb\r\n"));
        assertFalse(encoded.contains("$1\r\nc\r\n"));
    }

    @Test
    void execute_removesCountElementsFromStore() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act
        handler.execute(new String[]{"LPOP", "mylist", "2"});

        // Assert
        assertEquals(1, store.llen("mylist"));
        assertEquals("c", store.lpop("mylist").get());
    }

    @Test
    void execute_returnsAllElementsWhenCountExceedsListSize() {
        // Arrange
        store.rpush("mylist", List.of("x", "y"));

        // Act
        byte[] result = handler.execute(new String[]{"LPOP", "mylist", "100"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertEquals(0, store.llen("mylist"));
    }

    @Test
    void execute_returnsEmptyArrayForCountPopOnNonExistentKey() {
        // Arrange — key does not exist

        // Act
        byte[] result = handler.execute(new String[]{"LPOP", "ghost", "3"});

        // Assert — empty RESP array
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
