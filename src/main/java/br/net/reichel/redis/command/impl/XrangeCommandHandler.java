package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.command.CommandHandler;
import br.net.reichel.redis.model.StreamEntry;
import br.net.reichel.redis.protocol.RespEncoder;
import br.net.reichel.redis.store.DataStore;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handles the Redis XRANGE command.
 */
public class XrangeCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public XrangeCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Returns stream entries within an ID range, supporting "-" and "+" specials.
     *
     * @param args the command arguments; args[1]=key, args[2]=start, args[3]=end
     * @return the matching entries as a RESP array
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        String startArg = args[2];
        String endArg = args[3];

        long startMs, startSeq;
        if (startArg.equals("-")) {
            startMs = 0; startSeq = 0;
        } else if (startArg.contains("-")) {
            String[] p = startArg.split("-", 2);
            startMs = Long.parseLong(p[0]); startSeq = Long.parseLong(p[1]);
        } else {
            startMs = Long.parseLong(startArg); startSeq = 0;
        }

        long endMs, endSeq;
        if (endArg.equals("+")) {
            endMs = Long.MAX_VALUE; endSeq = Long.MAX_VALUE;
        } else if (endArg.contains("-")) {
            String[] p = endArg.split("-", 2);
            endMs = Long.parseLong(p[0]); endSeq = Long.parseLong(p[1]);
        } else {
            endMs = Long.parseLong(endArg); endSeq = Long.MAX_VALUE;
        }

        List<StreamEntry> results = store.xrange(key, startMs, startSeq, endMs, endSeq);
        return encodeEntries(results);
    }

    /**
     * Encodes a list of stream entries as a RESP array.
     *
     * @param entries the entries to encode
     * @return RESP-encoded bytes
     */
    public static byte[] encodeEntries(List<StreamEntry> entries) {
        StringBuilder sb = new StringBuilder("*").append(entries.size()).append("\r\n");
        for (StreamEntry entry : entries) {
            appendEntry(sb, entry);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Appends a single stream entry in RESP format to the given builder.
     *
     * @param sb    the string builder to append to
     * @param entry the stream entry to encode
     */
    public static void appendEntry(StringBuilder sb, StreamEntry entry) {
        sb.append("*2\r\n");
        sb.append("$").append(entry.id().length()).append("\r\n").append(entry.id()).append("\r\n");
        sb.append("*").append(entry.fields().size()).append("\r\n");
        for (String field : entry.fields()) {
            sb.append("$").append(field.length()).append("\r\n").append(field).append("\r\n");
        }
    }
}
