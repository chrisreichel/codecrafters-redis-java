package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.model.StreamEntry;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles the Redis XREAD command, including blocking mode and the "$" starting ID.
 */
public class XreadCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public XreadCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Reads entries from one or more streams, optionally blocking until data is available.
     *
     * @param args the command arguments
     * @return the matching entries as a RESP array, or null array on timeout
     */
    @Override
    public byte[] execute(String[] args) {
        boolean isBlock = args[1].equalsIgnoreCase("BLOCK");
        int streamsIdx = isBlock ? 3 : 1;
        long blockTimeoutMs = isBlock ? Long.parseLong(args[2]) : -1;
        int numStreams = (args.length - streamsIdx - 1) / 2;

        String[] keys = new String[numStreams];
        long[] afterMs = new long[numStreams];
        long[] afterSeq = new long[numStreams];

        for (int s = 0; s < numStreams; s++) {
            keys[s] = args[streamsIdx + 1 + s];
            String startArg = args[streamsIdx + 1 + numStreams + s];
            if (startArg.equals("$")) {
                Optional<StreamEntry> last = store.lastStreamEntry(keys[s]);
                if (last.isPresent()) {
                    afterMs[s] = last.get().millis();
                    afterSeq[s] = last.get().sequence();
                } else {
                    afterMs[s] = 0;
                    afterSeq[s] = -1;
                }
            } else if (startArg.contains("-")) {
                String[] p = startArg.split("-", 2);
                afterMs[s] = Long.parseLong(p[0]);
                afterSeq[s] = Long.parseLong(p[1]);
            } else {
                afterMs[s] = Long.parseLong(startArg);
                afterSeq[s] = Long.MAX_VALUE;
            }
        }

        return isBlock
                ? executeBlocking(keys, afterMs, afterSeq, numStreams, blockTimeoutMs)
                : executeNonBlocking(keys, afterMs, afterSeq, numStreams);
    }

    private byte[] executeNonBlocking(String[] keys, long[] afterMs, long[] afterSeq, int numStreams) {
        StringBuilder sb = new StringBuilder("*").append(numStreams).append("\r\n");
        for (int s = 0; s < numStreams; s++) {
            List<StreamEntry> results = store.xreadAfter(keys[s], afterMs[s], afterSeq[s]);
            sb.append("*2\r\n");
            sb.append("$").append(keys[s].length()).append("\r\n").append(keys[s]).append("\r\n");
            sb.append("*").append(results.size()).append("\r\n");
            for (StreamEntry entry : results) {
                XrangeCommandHandler.appendEntry(sb, entry);
            }
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private byte[] executeBlocking(String[] keys, long[] afterMs, long[] afterSeq, int numStreams, long timeoutMs) {
        List<Integer> nonEmpty = new ArrayList<>();
        List<StreamEntry>[] immediateResults = new ArrayList[numStreams];
        for (int s = 0; s < numStreams; s++) {
            immediateResults[s] = store.xreadAfter(keys[s], afterMs[s], afterSeq[s]);
            if (!immediateResults[s].isEmpty()) nonEmpty.add(s);
        }

        if (!nonEmpty.isEmpty()) {
            StringBuilder sb = new StringBuilder("*").append(nonEmpty.size()).append("\r\n");
            for (int idx : nonEmpty) {
                sb.append("*2\r\n");
                sb.append("$").append(keys[idx].length()).append("\r\n").append(keys[idx]).append("\r\n");
                sb.append("*").append(immediateResults[idx].size()).append("\r\n");
                for (StreamEntry entry : immediateResults[idx]) {
                    XrangeCommandHandler.appendEntry(sb, entry);
                }
            }
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        CompletableFuture<StreamEntry>[] futures = new CompletableFuture[numStreams];
        for (int s = 0; s < numStreams; s++) {
            futures[s] = new CompletableFuture<>();
            store.registerXreadWaiter(keys[s], futures[s]);
        }

        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(futures);
        try {
            if (timeoutMs == 0) {
                anyFuture.get();
            } else {
                anyFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
            }

            String resultKey = null;
            StreamEntry resultEntry = null;
            for (int s = 0; s < numStreams; s++) {
                if (futures[s].isDone() && !futures[s].isCancelled()) {
                    resultKey = keys[s];
                    resultEntry = futures[s].get();
                    break;
                }
            }
            for (int s = 0; s < numStreams; s++) {
                if (!futures[s].isDone()) {
                    futures[s].cancel(true);
                    store.removeXreadWaiter(keys[s], futures[s]);
                }
            }

            if (resultKey != null && resultEntry != null) {
                StringBuilder sb = new StringBuilder("*1\r\n*2\r\n");
                sb.append("$").append(resultKey.length()).append("\r\n").append(resultKey).append("\r\n");
                sb.append("*1\r\n");
                XrangeCommandHandler.appendEntry(sb, resultEntry);
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            }
            return RespEncoder.nullArray();

        } catch (TimeoutException e) {
            for (int s = 0; s < numStreams; s++) {
                futures[s].cancel(true);
                store.removeXreadWaiter(keys[s], futures[s]);
            }
            return RespEncoder.nullArray();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            Thread.currentThread().interrupt();
            return RespEncoder.error("ERR internal error");
        }
    }
}
