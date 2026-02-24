package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

/**
 * Handles the Redis SET command with optional PX/EX expiry options.
 */
public class SetCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public SetCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Stores a string value with an optional expiry.
     *
     * @param args the command arguments; args[1]=key, args[2]=value, optional PX/EX args
     * @return RESP simple string "+OK\r\n"
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        String value = args[2];
        long expiresAt = -1;
        for (int i = 3; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("PX")) {
                expiresAt = System.currentTimeMillis() + Long.parseLong(args[i + 1]);
            } else if (args[i].equalsIgnoreCase("EX")) {
                expiresAt = System.currentTimeMillis() + Long.parseLong(args[i + 1]) * 1000;
            }
        }
        store.setString(key, value, expiresAt);
        return RespEncoder.simpleString("OK");
    }
}
