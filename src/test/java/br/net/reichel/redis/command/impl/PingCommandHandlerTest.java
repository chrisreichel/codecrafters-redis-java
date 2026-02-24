package br.net.reichel.redis.command.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PingCommandHandler}.
 */
class PingCommandHandlerTest {

    private PingCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PingCommandHandler();
    }

    @Test
    void execute_returnsPongSimpleString() {
        // Arrange
        String[] args = {"PING"};

        // Act
        byte[] result = handler.execute(args);

        // Assert
        assertEquals("+PONG\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_withExtraArgs_stillReturnsPong() {
        // The PingCommandHandler ignores all arguments per Redis behaviour
        String[] args = {"PING", "hello"};

        byte[] result = handler.execute(args);

        assertEquals("+PONG\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsNonNullBytes() {
        byte[] result = handler.execute(new String[]{"PING"});
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
