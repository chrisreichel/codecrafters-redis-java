package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link XaddCommandHandler}.
 */
class XaddCommandHandlerTest {

    private InMemoryDataStore store;
    private XaddCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new XaddCommandHandler(store);
    }

    @Test
    void execute_returnsExplicitIdAsBulkString() {
        // Arrange & Act
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "100-1", "field", "value"});

        // Assert
        assertEquals("$5\r\n100-1\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_appendsEntryToStream() {
        // Arrange & Act
        handler.execute(new String[]{"XADD", "mystream", "200-0", "f", "v"});

        // Assert
        assertTrue(store.lastStreamEntry("mystream").isPresent());
        assertEquals("200-0", store.lastStreamEntry("mystream").get().id());
    }

    @Test
    void execute_autoGeneratesSequenceWhenStarSequence() {
        // Arrange — first entry for millisecond 1000; seq should become 0
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "1000-*", "f", "v"});

        // Assert — sequence part should be "0" since no prior entry for ms=1000
        String id = new String(result, StandardCharsets.UTF_8);
        assertTrue(id.contains("1000-0"));
    }

    @Test
    void execute_autoIncrementsSequenceForSameMillisecond() {
        // Arrange — add first entry at 500-0
        handler.execute(new String[]{"XADD", "mystream", "500-0", "a", "1"});

        // Act — add second entry with auto-sequence for same ms
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "500-*", "b", "2"});

        // Assert — sequence should be 1
        String id = new String(result, StandardCharsets.UTF_8);
        assertTrue(id.contains("500-1"));
    }

    @Test
    void execute_returnsErrorWhenIdIsZeroDashZero() {
        // Arrange & Act
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "0-0", "f", "v"});

        // Assert — RESP error begins with '-'
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("-"));
        assertTrue(encoded.contains("ERR"));
    }

    @Test
    void execute_returnsErrorWhenIdIsEqualToLastEntry() {
        // Arrange — add an entry at 300-5
        handler.execute(new String[]{"XADD", "mystream", "300-5", "f", "v"});

        // Act — attempt to add the same ID
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "300-5", "f2", "v2"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("-"));
        assertTrue(encoded.contains("ERR"));
    }

    @Test
    void execute_returnsErrorWhenIdIsSmallerThanLastEntry() {
        // Arrange
        handler.execute(new String[]{"XADD", "mystream", "1000-0", "f", "v"});

        // Act — attempt to add an older ID
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "500-0", "f2", "v2"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("-"));
        assertTrue(encoded.contains("ERR"));
    }

    @Test
    void execute_autoGeneratesSequenceAsOneForMillisZeroFirstEntry() {
        // Arrange — ms=0 requires seq >= 1 for the first auto-generated entry
        byte[] result = handler.execute(new String[]{"XADD", "mystream", "0-*", "f", "v"});

        // Assert
        String id = new String(result, StandardCharsets.UTF_8);
        assertTrue(id.contains("0-1"));
    }

    @Test
    void execute_storesFieldsInEntry() {
        // Arrange & Act
        handler.execute(new String[]{"XADD", "mystream", "999-0", "temperature", "25"});

        // Assert
        var entry = store.lastStreamEntry("mystream");
        assertTrue(entry.isPresent());
        assertTrue(entry.get().fields().contains("temperature"));
        assertTrue(entry.get().fields().contains("25"));
    }
}
