package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.command.CommandHandler;
import br.net.reichel.redis.protocol.RespEncoder;
import br.net.reichel.redis.store.DataStore;

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
