package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

/**
 * Handles the Redis TYPE command.
 */
public class TypeCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public TypeCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Returns the Redis type of the value stored at the given key.
     *
     * @param args the command arguments; args[1] is the key
     * @return the type as a RESP simple string ("string", "list", "stream", or "none")
     */
    @Override
    public byte[] execute(String[] args) {
        return RespEncoder.simpleString(store.type(args[1]));
    }
}
