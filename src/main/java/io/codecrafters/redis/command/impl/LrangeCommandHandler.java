package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.util.List;

/**
 * Handles the Redis LRANGE command.
 */
public class LrangeCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public LrangeCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Returns the sublist between start and stop indices (inclusive, negative indices supported).
     *
     * @param args the command arguments; args[1]=key, args[2]=start, args[3]=stop
     * @return the sublist as a RESP array of bulk strings
     */
    @Override
    public byte[] execute(String[] args) {
        List<String> result = store.lrange(
                args[1],
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]));
        return RespEncoder.bulkStringArray(result);
    }
}
