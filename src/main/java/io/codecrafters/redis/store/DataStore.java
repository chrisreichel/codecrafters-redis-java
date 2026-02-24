package io.codecrafters.redis.store;

import io.codecrafters.redis.model.StreamEntry;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Unified storage abstraction for all Redis data types.
 */
public interface DataStore {

    // --- String operations ---

    /**
     * Stores a string value with an optional expiry.
     *
     * @param key       the key
     * @param value     the string value
     * @param expiresAt epoch millis of expiry, or -1 for no expiry
     */
    void setString(String key, String value, long expiresAt);

    /**
     * Retrieves a non-expired string value.
     *
     * @param key the key
     * @return the value, or empty if absent or expired
     */
    Optional<String> getString(String key);

    /**
     * Returns the raw expiry epoch millis for a string key.
     * Used by INCR to preserve expiry on update.
     *
     * @param key the key
     * @return the expiry epoch millis, or -1 if absent or no expiry
     */
    long getExpiry(String key);

    // --- General ---

    /**
     * Returns the Redis type name for the value stored at key.
     *
     * @param key the key
     * @return "string", "list", "stream", or "none"
     */
    String type(String key);

    // --- List operations ---

    /**
     * Appends values to the tail of the list at key, notifying any blocked clients.
     *
     * @param key    the key
     * @param values values to append
     * @return the new list length
     */
    int rpush(String key, List<String> values);

    /**
     * Prepends values to the head of the list at key, notifying any blocked clients.
     *
     * @param key    the key
     * @param values values to prepend (each inserted at index 0 in order)
     * @return the new list length
     */
    int lpush(String key, List<String> values);

    /**
     * Returns the length of the list at key.
     *
     * @param key the key
     * @return the list length, or 0 if absent
     */
    int llen(String key);

    /**
     * Returns a sublist of the list at key.
     *
     * @param key   the key
     * @param start inclusive start index (negative indices supported)
     * @param stop  inclusive stop index (negative indices supported)
     * @return the sublist, possibly empty
     */
    List<String> lrange(String key, int start, int stop);

    /**
     * Pops one element from the head of the list.
     *
     * @param key the key
     * @return the popped value, or empty if the list is absent or empty
     */
    Optional<String> lpop(String key);

    /**
     * Pops up to count elements from the head of the list.
     *
     * @param key   the key
     * @param count maximum number of elements to pop
     * @return the popped values, possibly fewer than count
     */
    List<String> lpopN(String key, int count);

    /**
     * Atomically pops the head element if available, or registers a future waiter.
     * The future completes with [key, value] when an element is pushed.
     *
     * @param key    the key to watch
     * @param waiter the future to complete if no data is immediately available
     * @return [key, value] if data was immediately available, empty if waiter was registered
     */
    Optional<String[]> blpopImmediate(String key, CompletableFuture<String[]> waiter);

    /**
     * Removes a previously registered BLPOP waiter.
     *
     * @param key    the key
     * @param waiter the future to remove
     */
    void removeBlpopWaiter(String key, CompletableFuture<String[]> waiter);

    // --- Stream operations ---

    /**
     * Appends an entry to the stream at key and notifies any blocked XREAD clients.
     *
     * @param key        the stream key
     * @param resolvedId the fully resolved entry ID (e.g. "1234567890-0")
     * @param fields     alternating field/value pairs
     */
    void appendStreamEntry(String key, String resolvedId, List<String> fields);

    /**
     * Retrieves the last stream entry for the given key, if any.
     *
     * @param key the stream key
     * @return the last entry, or empty if the stream is absent or empty
     */
    Optional<StreamEntry> lastStreamEntry(String key);

    /**
     * Retrieves stream entries within an inclusive ID range.
     *
     * @param key      the stream key
     * @param startMs  start millisecond component (inclusive)
     * @param startSeq start sequence component (inclusive)
     * @param endMs    end millisecond component (inclusive)
     * @param endSeq   end sequence component (inclusive)
     * @return matching entries in order
     */
    List<StreamEntry> xrange(String key, long startMs, long startSeq, long endMs, long endSeq);

    /**
     * Retrieves stream entries with IDs strictly greater than the given ID.
     *
     * @param key     the stream key
     * @param afterMs millisecond component of the exclusive lower bound
     * @param afterSeq sequence component of the exclusive lower bound
     * @return matching entries in order
     */
    List<StreamEntry> xreadAfter(String key, long afterMs, long afterSeq);

    /**
     * Registers a future to be completed when a new entry is added to the stream.
     *
     * @param key    the stream key to watch
     * @param waiter the future to complete when a new entry arrives
     */
    void registerXreadWaiter(String key, CompletableFuture<StreamEntry> waiter);

    /**
     * Removes a previously registered XREAD waiter.
     *
     * @param key    the stream key
     * @param waiter the future to remove
     */
    void removeXreadWaiter(String key, CompletableFuture<StreamEntry> waiter);
}
