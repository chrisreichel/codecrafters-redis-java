package io.codecrafters.redis.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EchoCommandHandler}.
 */
class EchoCommandHandlerTest {

    private EchoCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EchoCommandHandler();
    }

    @Test
    void execute_returnsArgumentAsBulkString() {
        // Arrange
        String[] args = {"ECHO", "hello"};

        // Act
        byte[] result = handler.execute(args);

        // Assert
        assertEquals("$5\r\nhello\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsEmptyStringAsBulkString() {
        // Arrange
        String[] args = {"ECHO", ""};

        // Act
        byte[] result = handler.execute(args);

        // Assert
        assertEquals("$0\r\n\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello world", "foo", "a", "test message"})
    void execute_encodesArgumentWithCorrectLength(String message) {
        // Arrange
        String[] args = {"ECHO", message};

        // Act
        byte[] result = handler.execute(args);

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("$" + message.length() + "\r\n"));
        assertTrue(encoded.endsWith(message + "\r\n"));
    }

    @Test
    void execute_returnsNonNullBytes() {
        byte[] result = handler.execute(new String[]{"ECHO", "test"});
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
