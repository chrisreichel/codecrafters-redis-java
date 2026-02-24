package br.net.reichel.redis.model;

import java.util.List;

/**
 * An immutable Redis stream entry with a resolved ID and alternating field/value pairs.
 *
 * @param id     the entry ID in "milliseconds-sequence" format
 * @param fields alternating field/value pairs: [field1, value1, field2, value2, ...]
 */
public record StreamEntry(String id, List<String> fields) {

    /**
     * Returns the millisecond component of the entry ID.
     *
     * @return milliseconds
     */
    public long millis() {
        return Long.parseLong(id.split("-", 2)[0]);
    }

    /**
     * Returns the sequence component of the entry ID.
     *
     * @return sequence number
     */
    public long sequence() {
        return Long.parseLong(id.split("-", 2)[1]);
    }
}
