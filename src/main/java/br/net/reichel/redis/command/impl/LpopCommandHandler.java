package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.command.CommandHandler;
import br.net.reichel.redis.protocol.RespEncoder;
import br.net.reichel.redis.store.DataStore;

import java.util.List;

/**
 * Handles the Redis LPOP command (single and count variants).
 */
public class LpopCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public LpopCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Pops one or more elements from the head of the list.
     *
     * @param args the command arguments; args[1]=key, optional args[2]=count
     * @return bulk string for single pop, array for count pop, null bulk string if empty
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        if (args.length == 2) {
            return store.lpop(key)
                    .map(RespEncoder::bulkString)
                    .orElse(RespEncoder.nullBulkString());
        }
        List<String> popped = store.lpopN(key, Integer.parseInt(args[2]));
        return RespEncoder.bulkStringArray(popped);
    }
}
