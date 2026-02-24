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
        assertEquals("role:master", section);
    }

    @Test
    void toInfoSection_containsRoleKeyAndValue_forSlave() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("slave");

        // Act
        String section = info.toInfoSection();

        // Assert
        assertEquals("role:slave", section);
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
