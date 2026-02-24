package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

/**
 * Handles the Redis GET command.
 */
public class GetCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public GetCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Retrieves the value for the given key.
     *
     * @param args the command arguments; args[1] is the key
     * @return the value as a RESP bulk string, or null bulk string if absent or expired
     */
    @Override
    public byte[] execute(String[] args) {
        return store.getString(args[1])
                .map(RespEncoder::bulkString)
                .orElse(RespEncoder.nullBulkString());
    }
}
