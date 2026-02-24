package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.store.DataStore;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles the Redis BLPOP command with optional blocking timeout.
 */
public class BlpopCommandHandler implements CommandHandler {

    private final DataStore store;

    /**
     * Creates a handler backed by the given data store.
     *
     * @param store the data store
     */
    public BlpopCommandHandler(DataStore store) {
        this.store = store;
    }

    /**
     * Pops the head of a list, blocking until data is available or the timeout expires.
     *
     * @param args the command arguments; args[1]=key, args[last]=timeout (seconds, 0=infinite)
     * @return key and value as a RESP array, or null array on timeout
     */
    @Override
    public byte[] execute(String[] args) {
        String key = args[1];
        double timeout = Double.parseDouble(args[args.length - 1]);
        CompletableFuture<String[]> future = new CompletableFuture<>();
        Optional<String[]> immediate = store.blpopImmediate(key, future);
        if (immediate.isPresent()) {
            String[] r = immediate.get();
            return buildResponse(r[0], r[1]);
        }
        try {
            String[] result = timeout == 0
                    ? future.get()
                    : future.get((long) (timeout * 1000), TimeUnit.MILLISECONDS);
            return buildResponse(result[0], result[1]);
        } catch (TimeoutException e) {
            future.cancel(true);
            store.removeBlpopWaiter(key, future);
            return RespEncoder.nullArray();
        } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
            Thread.currentThread().interrupt();
            return RespEncoder.error("ERR internal error");
        }
    }

    private byte[] buildResponse(String key, String value) {
        String resp = "*2\r\n$" + key.length() + "\r\n" + key + "\r\n"
                + "$" + value.length() + "\r\n" + value + "\r\n";
        return resp.getBytes(StandardCharsets.UTF_8);
    }
}
