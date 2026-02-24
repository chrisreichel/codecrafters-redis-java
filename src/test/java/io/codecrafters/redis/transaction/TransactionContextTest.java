package io.codecrafters.redis.transaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionContext}.
 */
class TransactionContextTest {

    private TransactionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new TransactionContext();
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    void isActive_returnsFalse_beforeBegin() {
        assertFalse(ctx.isActive());
    }

    @Test
    void size_returnsZero_onNewContext() {
        assertEquals(0, ctx.size());
    }

    // -------------------------------------------------------------------------
    // begin
    // -------------------------------------------------------------------------

    @Test
    void begin_activatesTransaction() {
        ctx.begin();
        assertTrue(ctx.isActive());
    }

    // -------------------------------------------------------------------------
    // enqueue
    // -------------------------------------------------------------------------

    @Test
    void enqueue_incrementsSize() {
        ctx.begin();
        ctx.enqueue(new String[]{"SET", "k", "v"});
        assertEquals(1, ctx.size());
    }

    @Test
    void enqueue_multipleCommands_sizeReflectsCount() {
        ctx.begin();
        ctx.enqueue(new String[]{"SET", "a", "1"});
        ctx.enqueue(new String[]{"INCR", "a"});
        ctx.enqueue(new String[]{"GET", "a"});
        assertEquals(3, ctx.size());
    }

    // -------------------------------------------------------------------------
    // commitAndClear
    // -------------------------------------------------------------------------

    @Test
    void commitAndClear_returnsQueuedCommandsInOrder() {
        // Arrange
        ctx.begin();
        String[] cmd1 = {"SET", "key", "val"};
        String[] cmd2 = {"INCR", "counter"};
        ctx.enqueue(cmd1);
        ctx.enqueue(cmd2);

        // Act
        List<String[]> commands = ctx.commitAndClear();

        // Assert
        assertEquals(2, commands.size());
        assertArrayEquals(cmd1, commands.get(0));
        assertArrayEquals(cmd2, commands.get(1));
    }

    @Test
    void commitAndClear_deactivatesTransaction() {
        ctx.begin();
        ctx.enqueue(new String[]{"PING"});
        ctx.commitAndClear();

        assertFalse(ctx.isActive());
    }

    @Test
    void commitAndClear_clearsQueueAfterCommit() {
        ctx.begin();
        ctx.enqueue(new String[]{"SET", "k", "v"});
        ctx.commitAndClear();

        assertEquals(0, ctx.size());
    }

    @Test
    void commitAndClear_returnsEmptyList_whenNoCommandsQueued() {
        ctx.begin();
        List<String[]> commands = ctx.commitAndClear();
        assertTrue(commands.isEmpty());
    }

    @Test
    void commitAndClear_returnedListIsSnapshot_notLiveReference() {
        // Arrange
        ctx.begin();
        ctx.enqueue(new String[]{"PING"});

        // Act — get snapshot before clearing
        List<String[]> snapshot = ctx.commitAndClear();

        // Attempt to enqueue more and commit again does not affect previous snapshot
        ctx.begin();
        ctx.enqueue(new String[]{"SET", "k", "v"});
        ctx.commitAndClear();

        // Assert — original snapshot still has just 1 entry
        assertEquals(1, snapshot.size());
    }

    // -------------------------------------------------------------------------
    // rollback
    // -------------------------------------------------------------------------

    @Test
    void rollback_deactivatesTransaction() {
        ctx.begin();
        ctx.rollback();
        assertFalse(ctx.isActive());
    }

    @Test
    void rollback_discardsQueuedCommands() {
        ctx.begin();
        ctx.enqueue(new String[]{"SET", "k", "v"});
        ctx.enqueue(new String[]{"GET", "k"});

        ctx.rollback();

        assertEquals(0, ctx.size());
    }

    @Test
    void rollback_onEmptyQueue_doesNotThrow() {
        ctx.begin();
        assertDoesNotThrow(() -> ctx.rollback());
        assertFalse(ctx.isActive());
    }

    // -------------------------------------------------------------------------
    // Re-use after commit/rollback
    // -------------------------------------------------------------------------

    @Test
    void canBeginAgainAfterCommit() {
        ctx.begin();
        ctx.commitAndClear();
        ctx.begin();
        assertTrue(ctx.isActive());
    }

    @Test
    void canBeginAgainAfterRollback() {
        ctx.begin();
        ctx.rollback();
        ctx.begin();
        assertTrue(ctx.isActive());
    }
}
