package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.model.StreamEntry;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Handles the Redis XADD command with full, partial, and fully auto-generated IDs.
 */
public class XaddCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public XaddCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Appends a new entry to a stream, resolving auto-generated ID components as needed.
     *
     * @param args the command arguments; args[1]=key, args[2]=id, args[3..]=field/value pairs
     * @return the assigned entry ID as a RESP bulk string, or an error
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        String id = args[2];

        if (id.equals("*")) {
            id = System.currentTimeMillis() + "-*";
        }

        String[] idParts = id.split("-");
        long ms = Long.parseLong(idParts[0]);
        long seq;

        if (idParts[1].equals("*")) {
            Optional<StreamEntry> last = store.lastStreamEntry(key);
            long lastSeqForMs = -1;
            if (last.isPresent() && last.get().millis() == ms) {
                lastSeqForMs = last.get().sequence();
            }
            seq = (lastSeqForMs == -1) ? (ms == 0 ? 1 : 0) : lastSeqForMs + 1;
            id = ms + "-" + seq;
        } else {
            seq = Long.parseLong(idParts[1]);
            if (ms == 0 && seq == 0) {
                return RespEncoder.error("ERR The ID specified in XADD must be greater than 0-0");
            }
        }

        Optional<StreamEntry> last = store.lastStreamEntry(key);
        if (last.isPresent()) {
            long lastMs = last.get().millis();
            long lastSeq = last.get().sequence();
            if (ms < lastMs || (ms == lastMs && seq <= lastSeq)) {
                return RespEncoder.error("ERR The ID specified in XADD is equal or smaller than the target stream top item");
            }
        }

        List<String> fields = Arrays.asList(args).subList(3, args.length);
        store.appendStreamEntry(key, id, fields);
        return RespEncoder.bulkString(id);
    }
}
