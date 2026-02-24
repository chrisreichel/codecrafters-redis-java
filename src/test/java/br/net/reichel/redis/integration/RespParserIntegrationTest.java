package br.net.reichel.redis.integration;

import br.net.reichel.redis.protocol.RespParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RespParser}.
 */
class RespParserIntegrationTest {

    @Test
    void readCommand_parsesSimpleCommand() throws IOException {
        String input = "*1\r\n$4\r\nPING\r\n";
        RespParser parser = new RespParser(new BufferedReader(new StringReader(input)));

        Optional<String[]> result = parser.readCommand();

        assertTrue(result.isPresent());
        assertArrayEquals(new String[]{"PING"}, result.get());
    }

    @Test
    void readCommand_parsesMultiTokenCommand() throws IOException {
        String input = "*3\r\n$3\r\nSET\r\n$1\r\nk\r\n$1\r\nv\r\n";
        RespParser parser = new RespParser(new BufferedReader(new StringReader(input)));

        Optional<String[]> result = parser.readCommand();

        assertTrue(result.isPresent());
        assertArrayEquals(new String[]{"SET", "k", "v"}, result.get());
    }

    @Test
    void readCommand_returnsEmpty_onEof() throws IOException {
        RespParser parser = new RespParser(new BufferedReader(new StringReader("")));

        Optional<String[]> result = parser.readCommand();

        assertTrue(result.isEmpty());
    }

    @Test
    void readCommand_returnsEmpty_onNonArrayFrame() throws IOException {
        // A simple string frame, not an array
        String input = "+OK\r\n";
        RespParser parser = new RespParser(new BufferedReader(new StringReader(input)));

        Optional<String[]> result = parser.readCommand();

        assertTrue(result.isEmpty());
    }
}
