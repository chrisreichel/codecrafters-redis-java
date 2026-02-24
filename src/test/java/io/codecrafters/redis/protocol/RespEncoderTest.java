package io.codecrafters.redis.protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link RespEncoder}.
 */
class RespEncoderTest {

    // -------------------------------------------------------------------------
    // simpleString
    // -------------------------------------------------------------------------

    @Test
    void simpleString_encodesWithPlusPrefix() {
        // Arrange
        String value = "OK";

        // Act
        byte[] result = RespEncoder.simpleString(value);

        // Assert
        assertEquals("+OK\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void simpleString_encodesEmptyString() {
        byte[] result = RespEncoder.simpleString("");
        assertEquals("+\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void simpleString_encodesPong() {
        byte[] result = RespEncoder.simpleString("PONG");
        assertEquals("+PONG\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // error
    // -------------------------------------------------------------------------

    @Test
    void error_encodesWithDashPrefix() {
        // Arrange
        String message = "ERR unknown command";

        // Act
        byte[] result = RespEncoder.error(message);

        // Assert
        assertEquals("-ERR unknown command\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void error_encodesEmptyMessage() {
        byte[] result = RespEncoder.error("");
        assertEquals("-\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // integer
    // -------------------------------------------------------------------------

    @Test
    void integer_encodesPositiveValue() {
        byte[] result = RespEncoder.integer(42L);
        assertEquals(":42\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void integer_encodesZero() {
        byte[] result = RespEncoder.integer(0L);
        assertEquals(":0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void integer_encodesNegativeValue() {
        byte[] result = RespEncoder.integer(-1L);
        assertEquals(":-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void integer_encodesLargeValue() {
        byte[] result = RespEncoder.integer(Long.MAX_VALUE);
        assertEquals(":" + Long.MAX_VALUE + "\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // bulkString
    // -------------------------------------------------------------------------

    @Test
    void bulkString_encodesWithLengthPrefix() {
        // Arrange
        String value = "hello";

        // Act
        byte[] result = RespEncoder.bulkString(value);

        // Assert
        assertEquals("$5\r\nhello\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void bulkString_encodesEmptyString() {
        byte[] result = RespEncoder.bulkString("");
        assertEquals("$0\r\n\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void bulkString_lengthMatchesActualValue() {
        String value = "hello world";
        byte[] result = RespEncoder.bulkString(value);
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("$" + value.length() + "\r\n"));
    }

    // -------------------------------------------------------------------------
    // nullBulkString
    // -------------------------------------------------------------------------

    @Test
    void nullBulkString_encodesCorrectly() {
        byte[] result = RespEncoder.nullBulkString();
        assertEquals("$-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // nullArray
    // -------------------------------------------------------------------------

    @Test
    void nullArray_encodesCorrectly() {
        byte[] result = RespEncoder.nullArray();
        assertEquals("*-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // emptyArray
    // -------------------------------------------------------------------------

    @Test
    void emptyArray_encodesCorrectly() {
        byte[] result = RespEncoder.emptyArray();
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // bulkStringArray
    // -------------------------------------------------------------------------

    @Test
    void bulkStringArray_encodesSingleElement() {
        // Arrange
        List<String> values = List.of("hello");

        // Act
        byte[] result = RespEncoder.bulkStringArray(values);

        // Assert
        assertEquals("*1\r\n$5\r\nhello\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void bulkStringArray_encodesMultipleElements() {
        // Arrange
        List<String> values = List.of("foo", "bar", "baz");

        // Act
        byte[] result = RespEncoder.bulkStringArray(values);

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertEquals("*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n", encoded);
    }

    @Test
    void bulkStringArray_encodesEmptyList() {
        byte[] result = RespEncoder.bulkStringArray(List.of());
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PING", "SET", "GET", "DEL"})
    void bulkStringArray_countPrefixMatchesElementCount(String command) {
        List<String> values = List.of(command, "key", "value");
        byte[] result = RespEncoder.bulkStringArray(values);
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*3\r\n"));
    }
}
