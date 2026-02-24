package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

/**
 * Handles the Redis LLEN command.
 */
public class LlenCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public LlenCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Returns the length of the list at the given key.
     *
     * @param args the command arguments; args[1] is the key
     * @return the list length as a RESP integer, or 0 if the key does not exist
     */
    @Override
    public byte[] execute(String[] args) {
        return RespEncoder.integer(store.llen(args[1]));
    }
}
