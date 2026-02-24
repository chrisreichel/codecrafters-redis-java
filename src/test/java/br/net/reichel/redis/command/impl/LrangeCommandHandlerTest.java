package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LrangeCommandHandler}.
 */
class LrangeCommandHandlerTest {

    private InMemoryDataStore store;
    private LrangeCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new LrangeCommandHandler(store);
    }

    @Test
    void execute_returnsEmptyArrayForNonExistentKey() {
        // Arrange — key does not exist

        // Act
        byte[] result = handler.execute(new String[]{"LRANGE", "ghost", "0", "-1"});

        // Assert — empty RESP array
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsAllElementsWithZeroToMinusOne() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act
        byte[] result = handler.execute(new String[]{"LRANGE", "mylist", "0", "-1"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("$1\r\na\r\n"));
        assertTrue(encoded.contains("$1\r\nb\r\n"));
        assertTrue(encoded.contains("$1\r\nc\r\n"));
        assertTrue(encoded.startsWith("*3\r\n"));
    }

    @Test
    void execute_returnsSubrangeForPositiveIndices() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c", "d"));

        // Act
        byte[] result = handler.execute(new String[]{"LRANGE", "mylist", "1", "2"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertTrue(encoded.contains("$1\r\nb\r\n"));
        assertTrue(encoded.contains("$1\r\nc\r\n"));
        assertFalse(encoded.contains("$1\r\na\r\n"));
        assertFalse(encoded.contains("$1\r\nd\r\n"));
    }

    @Test
    void execute_returnsEmptyArrayWhenStartExceedsListSize() {
        // Arrange
        store.rpush("mylist", List.of("a", "b"));

        // Act
        byte[] result = handler.execute(new String[]{"LRANGE", "mylist", "10", "20"});

        // Assert
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_handlesNegativeStartIndex() {
        // Arrange
        store.rpush("mylist", List.of("a", "b", "c"));

        // Act — -2 maps to index 1 ("b")
        byte[] result = handler.execute(new String[]{"LRANGE", "mylist", "-2", "-1"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertTrue(encoded.contains("$1\r\nb\r\n"));
        assertTrue(encoded.contains("$1\r\nc\r\n"));
    }

    @Test
    void execute_returnsSingleElementWhenStartEqualsStop() {
        // Arrange
        store.rpush("mylist", List.of("x", "y", "z"));

        // Act
        byte[] result = handler.execute(new String[]{"LRANGE", "mylist", "1", "1"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*1\r\n"));
        assertTrue(encoded.contains("$1\r\ny\r\n"));
    }
}
