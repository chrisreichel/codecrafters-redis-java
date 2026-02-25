package br.net.reichel.redis.replication.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StandaloneReplicationInfo}.
 */
class StandaloneReplicationInfoTest {

    @Test
    void getRole_returnsMaster_whenConstructedWithMaster() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");

        // Act & Assert
        assertEquals("master", info.getRole());
    }

    @Test
    void getRole_returnsSlave_whenConstructedWithSlave() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("slave");

        // Act & Assert
        assertEquals("slave", info.getRole());
    }

    @ParameterizedTest
    @ValueSource(strings = {"master", "slave", "replica"})
    void getRole_returnsExactRolePassedToConstructor(String role) {
        StandaloneReplicationInfo info = new StandaloneReplicationInfo(role);
        assertEquals(role, info.getRole());
    }

    @Test
    void toInfoSection_containsRoleKeyAndValue_forMaster() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");

        // Act
        String section = info.toInfoSection();

        // Assert
        assertTrue(section.contains("role:master"));
        assertTrue(section.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        assertTrue(section.contains("master_repl_offset:0"));
    }

    @Test
    void toInfoSection_containsRoleKeyAndValue_forSlave() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("slave");

        // Act
        String section = info.toInfoSection();

        // Assert
        assertTrue(section.contains("role:slave"));
        assertTrue(section.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"));
        assertTrue(section.contains("master_repl_offset:0"));
    }

    @Test
    void getMasterReplId_returnsHardcodedValue() {
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");
        assertEquals("8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb", info.getMasterReplId());
    }

    @Test
    void getMasterReplOffset_returnsZero() {
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");
        assertEquals(0, info.getMasterReplOffset());
    }

    @Test
    void toInfoSection_returnsNonNullString() {
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");
        assertNotNull(info.toInfoSection());
    }

    @Test
    void toInfoSection_containsRolePrefix() {
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");
        assertTrue(info.toInfoSection().startsWith("role:"));
    }
}
