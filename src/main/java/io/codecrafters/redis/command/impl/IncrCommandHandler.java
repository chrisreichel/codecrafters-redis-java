package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.util.Optional;

/**
 * Handles the Redis INCR command.
 */
public class IncrCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public IncrCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Increments the integer value at the given key by 1.
     * If the key does not exist, it is initialized to 0 before incrementing.
     *
     * @param args the command arguments; args[1] is the key
     * @return the new integer value as a RESP integer, or an error if not numeric
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        Optional<String> current = store.getString(key);
        long val = 0;
        if (current.isPresent()) {
            try {
                val = Long.parseLong(current.get());
            } catch (NumberFormatException e) {
                return RespEncoder.error("ERR value is not an integer or out of range");
            }
        }
        val++;
        long expiry = current.isPresent() ? store.getExpiry(key) : -1;
        store.setString(key, String.valueOf(val), expiry);
        return RespEncoder.integer(val);
    }
}
