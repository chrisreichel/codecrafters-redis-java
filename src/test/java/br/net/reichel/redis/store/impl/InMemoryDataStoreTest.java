package br.net.reichel.redis.store.impl;

import br.net.reichel.redis.model.StreamEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InMemoryDataStore}.
 */
class InMemoryDataStoreTest {

    private InMemoryDataStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
    }

    // -------------------------------------------------------------------------
    // String operations
    // -------------------------------------------------------------------------

    @Test
    void setString_andGetString_returnsStoredValue() {
        // Arrange & Act
        store.setString("key", "value", -1);

        // Assert
        Optional<String> result = store.getString("key");
        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    void getString_returnsEmpty_whenKeyDoesNotExist() {
        Optional<String> result = store.getString("missing");
        assertFalse(result.isPresent());
    }

    @Test
    void getString_returnsEmpty_whenKeyHasExpired() throws InterruptedException {
        // Arrange — expires in 50 ms
        store.setString("temp", "val", System.currentTimeMillis() + 50);

        Thread.sleep(100);

        // Assert — key is gone
        assertFalse(store.getString("temp").isPresent());
    }

    @Test
    void getString_returnsValue_whenKeyHasNotYetExpired() {
        // Arrange — expires in 60 seconds
        store.setString("future", "v", System.currentTimeMillis() + 60_000);

        assertTrue(store.getString("future").isPresent());
    }

    @Test
    void getExpiry_returnsMinusOne_whenNoExpiry() {
        store.setString("noexp", "v", -1);
        assertEquals(-1L, store.getExpiry("noexp"));
    }

    @Test
    void getExpiry_returnsMinusOne_whenKeyMissing() {
        assertEquals(-1L, store.getExpiry("ghost"));
    }

    @Test
    void getExpiry_returnsExpiryTimestamp_whenSet() {
        long expiry = System.currentTimeMillis() + 5_000;
        store.setString("timed", "v", expiry);
        assertEquals(expiry, store.getExpiry("timed"));
    }

    @Test
    void setString_overwritesPreviousValue() {
        store.setString("k", "old", -1);
        store.setString("k", "new", -1);
        assertEquals("new", store.getString("k").get());
    }

    // -------------------------------------------------------------------------
    // type
    // -------------------------------------------------------------------------

    @Test
    void type_returnsNone_whenKeyDoesNotExist() {
        assertEquals("none", store.type("absent"));
    }

    @Test
    void type_returnsString_afterSetString() {
        store.setString("s", "v", -1);
        assertEquals("string", store.type("s"));
    }

    @Test
    void type_returnsList_afterRpush() {
        store.rpush("l", List.of("a"));
        assertEquals("list", store.type("l"));
    }

    @Test
    void type_returnsStream_afterAppendStreamEntry() {
        store.appendStreamEntry("stream", "100-0", List.of("f", "v"));
        assertEquals("stream", store.type("stream"));
    }

    // -------------------------------------------------------------------------
    // List — rpush / lpush / llen / lrange
    // -------------------------------------------------------------------------

    @Test
    void rpush_appendsValuesAndReturnsNewLength() {
        int size = store.rpush("l", List.of("a", "b", "c"));
        assertEquals(3, size);
    }

    @Test
    void rpush_appendsToExistingList() {
        store.rpush("l", List.of("a"));
        int size = store.rpush("l", List.of("b", "c"));
        assertEquals(3, size);
    }

    @Test
    void lpush_prependsValuesToHeadAndReturnsNewLength() {
        int size = store.lpush("l", List.of("a", "b"));
        assertEquals(2, size);
    }

    @Test
    void llen_returnsZero_whenKeyDoesNotExist() {
        assertEquals(0, store.llen("nope"));
    }

    @Test
    void llen_returnsCorrectLength_afterPush() {
        store.rpush("l", List.of("x", "y", "z"));
        assertEquals(3, store.llen("l"));
    }

    @Test
    void lrange_returnsElements_forValidRange() {
        store.rpush("l", List.of("a", "b", "c", "d"));
        List<String> result = store.lrange("l", 0, 2);
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void lrange_returnsEmptyList_whenKeyDoesNotExist() {
        List<String> result = store.lrange("missing", 0, -1);
        assertTrue(result.isEmpty());
    }

    @Test
    void lrange_supportsNegativeIndices() {
        store.rpush("l", List.of("a", "b", "c"));
        // -1 == last element
        List<String> result = store.lrange("l", 0, -1);
        assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    void lrange_returnsEmptyList_whenStartExceedsSize() {
        store.rpush("l", List.of("a", "b"));
        List<String> result = store.lrange("l", 10, 20);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // List — lpop / lpopN
    // -------------------------------------------------------------------------

    @Test
    void lpop_removesAndReturnsHeadElement() {
        store.rpush("l", List.of("first", "second"));
        Optional<String> popped = store.lpop("l");
        assertTrue(popped.isPresent());
        assertEquals("first", popped.get());
    }

    @Test
    void lpop_returnsEmpty_whenListIsEmpty() {
        Optional<String> result = store.lpop("empty");
        assertFalse(result.isPresent());
    }

    @Test
    void lpopN_removesUpToCountElements() {
        store.rpush("l", List.of("a", "b", "c", "d"));
        List<String> popped = store.lpopN("l", 2);
        assertEquals(List.of("a", "b"), popped);
        assertEquals(2, store.llen("l"));
    }

    @Test
    void lpopN_returnsAllElements_whenCountExceedsListSize() {
        store.rpush("l", List.of("x", "y"));
        List<String> popped = store.lpopN("l", 100);
        assertEquals(List.of("x", "y"), popped);
        assertEquals(0, store.llen("l"));
    }

    @Test
    void lpopN_returnsEmptyList_whenKeyDoesNotExist() {
        List<String> result = store.lpopN("ghost", 3);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Stream — appendStreamEntry / lastStreamEntry / xrange / xreadAfter
    // -------------------------------------------------------------------------

    @Test
    void appendStreamEntry_andLastStreamEntry_returnsLastEntry() {
        store.appendStreamEntry("s", "1000-0", List.of("f1", "v1"));
        store.appendStreamEntry("s", "2000-0", List.of("f2", "v2"));

        Optional<StreamEntry> last = store.lastStreamEntry("s");
        assertTrue(last.isPresent());
        assertEquals("2000-0", last.get().id());
    }

    @Test
    void lastStreamEntry_returnsEmpty_whenStreamDoesNotExist() {
        assertFalse(store.lastStreamEntry("nostream").isPresent());
    }

    @Test
    void xrange_returnsEntriesWithinInclusiveRange() {
        store.appendStreamEntry("s", "1000-0", List.of("a", "1"));
        store.appendStreamEntry("s", "2000-0", List.of("b", "2"));
        store.appendStreamEntry("s", "3000-0", List.of("c", "3"));

        List<StreamEntry> result = store.xrange("s", 1000, 0, 2000, 0);
        assertEquals(2, result.size());
        assertEquals("1000-0", result.get(0).id());
        assertEquals("2000-0", result.get(1).id());
    }

    @Test
    void xrange_returnsEmptyList_whenStreamDoesNotExist() {
        List<StreamEntry> result = store.xrange("none", 0, 0, Long.MAX_VALUE, Long.MAX_VALUE);
        assertTrue(result.isEmpty());
    }

    @Test
    void xreadAfter_returnsEntriesWithIdGreaterThanGiven() {
        store.appendStreamEntry("s", "1000-0", List.of("a", "1"));
        store.appendStreamEntry("s", "2000-0", List.of("b", "2"));
        store.appendStreamEntry("s", "3000-0", List.of("c", "3"));

        List<StreamEntry> result = store.xreadAfter("s", 1000, 0);
        assertEquals(2, result.size());
        assertEquals("2000-0", result.get(0).id());
        assertEquals("3000-0", result.get(1).id());
    }

    @Test
    void xreadAfter_returnsEmptyList_whenNoEntriesAfterGivenId() {
        store.appendStreamEntry("s", "1000-0", List.of("a", "1"));

        List<StreamEntry> result = store.xreadAfter("s", 1000, 0);
        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // BLPOP waiter registration / removal
    // -------------------------------------------------------------------------

    @Test
    void blpopImmediate_returnsValue_whenListIsNonEmpty() {
        store.rpush("q", List.of("item"));
        CompletableFuture<String[]> future = new CompletableFuture<>();

        Optional<String[]> result = store.blpopImmediate("q", future);

        assertTrue(result.isPresent());
        assertArrayEquals(new String[]{"q", "item"}, result.get());
    }

    @Test
    void blpopImmediate_registersWaiter_whenListIsEmpty() {
        CompletableFuture<String[]> future = new CompletableFuture<>();

        Optional<String[]> result = store.blpopImmediate("q", future);

        assertFalse(result.isPresent());
        assertFalse(future.isDone());
    }

    @Test
    void blpopImmediate_waiterCompletedWhenValuePushed() throws Exception {
        CompletableFuture<String[]> future = new CompletableFuture<>();
        store.blpopImmediate("q", future);

        // Push triggers the waiter
        store.rpush("q", List.of("pushed"));

        String[] completed = future.get();
        assertArrayEquals(new String[]{"q", "pushed"}, completed);
    }
}
