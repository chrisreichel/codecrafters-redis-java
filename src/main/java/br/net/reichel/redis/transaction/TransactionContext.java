package br.net.reichel.redis.transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the state of an in-progress MULTI/EXEC transaction for a single client connection.
 */
public class TransactionContext {

    private boolean active = false;
    private final List<String[]> queue = new ArrayList<>();

    /**
     * Starts the transaction.
     */
    public void begin() {
        active = true;
    }

    /**
     * Returns whether a MULTI block is currently open.
     *
     * @return true if in a MULTI block
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the number of queued commands.
     *
     * @return queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * Enqueues a command for deferred execution.
     *
     * @param command the tokenized command to queue
     */
    public void enqueue(String[] command) {
        queue.add(command);
    }

    /**
     * Drains and returns all queued commands, then closes the transaction.
     *
     * @return the list of queued commands in insertion order
     */
    public List<String[]> commitAndClear() {
        List<String[]> snapshot = new ArrayList<>(queue);
        queue.clear();
        active = false;
        return snapshot;
    }

    /**
     * Discards all queued commands and closes the transaction.
     */
    public void rollback() {
        queue.clear();
        active = false;
    }
}
