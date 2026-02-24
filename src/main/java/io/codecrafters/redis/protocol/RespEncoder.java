package io.codecrafters.redis.protocol;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Encodes Java values into RESP (REdis Serialization Protocol) wire-format bytes.
 */
public final class RespEncoder {

    private RespEncoder() {}

    /**
     * Encodes a RESP simple string: +value\r\n
     *
     * @param value the string value
     * @return encoded bytes
     */
    public static byte[] simpleString(String value) {
        return ("+" + value + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP error: -message\r\n
     *
     * @param message the error message
     * @return encoded bytes
     */
    public static byte[] error(String message) {
        return ("-" + message + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP integer: :value\r\n
     *
     * @param value the integer value
     * @return encoded bytes
     */
    public static byte[] integer(long value) {
        return (":" + value + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP bulk string: $length\r\nvalue\r\n
     *
     * @param value the string value
     * @return encoded bytes
     */
    public static byte[] bulkString(String value) {
        return ("$" + value.length() + "\r\n" + value + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP null bulk string: $-1\r\n
     *
     * @return encoded bytes
     */
    public static byte[] nullBulkString() {
        return "$-1\r\n".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP null array: *-1\r\n
     *
     * @return encoded bytes
     */
    public static byte[] nullArray() {
        return "*-1\r\n".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP empty array: *0\r\n
     *
     * @return encoded bytes
     */
    public static byte[] emptyArray() {
        return "*0\r\n".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a RESP array of bulk strings.
     *
     * @param values the string values
     * @return encoded bytes
     */
    public static byte[] bulkStringArray(List<String> values) {
        StringBuilder sb = new StringBuilder("*").append(values.size()).append("\r\n");
        for (String v : values) {
            sb.append("$").append(v.length()).append("\r\n").append(v).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
