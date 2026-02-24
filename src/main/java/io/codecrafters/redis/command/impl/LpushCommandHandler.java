package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.util.Arrays;
import java.util.List;

/**
 * Handles the Redis LPUSH command.
 */
public class LpushCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public LpushCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Prepends values to the head of the list at the given key.
     *
     * @param args the command arguments; args[1]=key, args[2..]=values
     * @return the new list length as a RESP integer
     */
    @Override
    public byte[] execute(String[] args) {
        List<String> values = Arrays.asList(args).subList(2, args.length);
        return RespEncoder.integer(store.lpush(args[1], values));
    }
}
