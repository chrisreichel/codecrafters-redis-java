package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link XrangeCommandHandler}.
 */
class XrangeCommandHandlerTest {

    private InMemoryDataStore store;
    private XrangeCommandHandler handler;

    @BeforeEach
    void setUp() {
        store = new InMemoryDataStore();
        handler = new XrangeCommandHandler(store);
    }

    @Test
    void execute_returnsEmptyArrayForNonExistentStream() {
        // Arrange — no stream exists

        // Act
        byte[] result = handler.execute(new String[]{"XRANGE", "missing", "-", "+"});

        // Assert
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    void execute_returnsAllEntriesWithMinusToPlus() {
        // Arrange
        store.appendStreamEntry("s", "1000-0", List.of("a", "1"));
        store.appendStreamEntry("s", "2000-0", List.of("b", "2"));

        // Act
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "-", "+"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertTrue(encoded.contains("1000-0"));
        assertTrue(encoded.contains("2000-0"));
    }

    @Test
    void execute_returnsEntriesWithExplicitFullIdRange() {
        // Arrange
        store.appendStreamEntry("s", "1000-0", List.of("f1", "v1"));
        store.appendStreamEntry("s", "2000-0", List.of("f2", "v2"));
        store.appendStreamEntry("s", "3000-0", List.of("f3", "v3"));

        // Act — range from 1000-0 to 2000-0
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "1000-0", "2000-0"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
        assertTrue(encoded.contains("1000-0"));
        assertTrue(encoded.contains("2000-0"));
        assertFalse(encoded.contains("3000-0"));
    }

    @Test
    void execute_returnsEntriesWhenStartIsMillisOnlyWithoutSequence() {
        // Arrange
        store.appendStreamEntry("s", "500-0", List.of("f", "v"));
        store.appendStreamEntry("s", "1500-0", List.of("g", "w"));

        // Act — start without "-seq" part defaults to seq=0
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "500", "1500"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*2\r\n"));
    }

    @Test
    void execute_returnsOnlyStartEntryWhenRangeIsTight() {
        // Arrange
        store.appendStreamEntry("s", "100-0", List.of("f", "v"));
        store.appendStreamEntry("s", "200-0", List.of("g", "w"));

        // Act
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "100-0", "100-0"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.startsWith("*1\r\n"));
        assertTrue(encoded.contains("100-0"));
        assertFalse(encoded.contains("200-0"));
    }

    @Test
    void execute_encodesFieldsInsideEachEntry() {
        // Arrange
        store.appendStreamEntry("s", "300-0", List.of("temp", "42"));

        // Act
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "-", "+"});

        // Assert — entry fields should appear in the response
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("temp"));
        assertTrue(encoded.contains("42"));
    }

    @Test
    void execute_returnsEmptyArrayWhenNoEntriesMatchRange() {
        // Arrange — entries exist but outside the queried range
        store.appendStreamEntry("s", "5000-0", List.of("f", "v"));

        // Act
        byte[] result = handler.execute(new String[]{"XRANGE", "s", "1000-0", "2000-0"});

        // Assert
        assertEquals("*0\r\n", new String(result, StandardCharsets.UTF_8));
    }
}
