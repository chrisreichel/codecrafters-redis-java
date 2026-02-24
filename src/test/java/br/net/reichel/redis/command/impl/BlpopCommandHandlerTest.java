package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BlpopCommandHandler}.
 * Only the non-blocking path (data already present) is exercised here.
 */
class BlpopCommandHandlerTest {

    private InMemoryDataStore store;
    private BlpopCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new BlpopCommandHandler(store);
    }

    @Test
    void execute_returnsKeyAndValueImmediatelyWhenListHasData() {
        // Arrange
        store.rpush("queue", List.of("job1"));

        // Act
        byte[] result = handler.execute(new String[]{"BLPOP", "queue", "0"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("queue"));
        assertTrue(encoded.contains("job1"));
        assertTrue(encoded.startsWith("*2\r\n"));
    }

    @Test
    void execute_removesHeadElementFromListWhenReturningImmediately() {
        // Arrange
        store.rpush("queue", List.of("a", "b"));

        // Act
        handler.execute(new String[]{"BLPOP", "queue", "0"});

        // Assert — "a" was consumed; only "b" remains
        assertEquals(1, store.llen("queue"));
        assertEquals("b", store.lpop("queue").get());
    }

    @Test
    void execute_returnsNullArrayOnTimeoutWhenListIsEmpty() {
        // Arrange — no data, short timeout of 100 ms
        // Act
        byte[] result = handler.execute(new String[]{"BLPOP", "empty", "0.1"});

        // Assert
        assertEquals("*-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_responseContainsCorrectKeyName() {
        // Arrange
        store.rpush("mykey", List.of("val"));

        // Act
        byte[] result = handler.execute(new String[]{"BLPOP", "mykey", "0"});

        // Assert — key name is encoded as a bulk string in the response
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("$5\r\nmykey\r\n"));
    }

    @Test
    void execute_responseContainsCorrectValue() {
        // Arrange
        store.rpush("k", List.of("hello"));

        // Act
        byte[] result = handler.execute(new String[]{"BLPOP", "k", "0"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("$5\r\nhello\r\n"));
    }
}
