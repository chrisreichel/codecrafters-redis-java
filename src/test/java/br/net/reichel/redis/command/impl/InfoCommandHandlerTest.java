package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.replication.ReplicationInfo;
import br.net.reichel.redis.replication.impl.StandaloneReplicationInfo;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InfoCommandHandler}.
 */
class InfoCommandHandlerTest {

    // -------------------------------------------------------------------------
    // Helper: minimal stub for ReplicationInfo
    // -------------------------------------------------------------------------

    private static ReplicationInfo stubReplicationInfo(String role, String infoSection) {
        return new ReplicationInfo() {
            @Override
            public String getRole() {
                return role;
            }

            @Override
            public String getMasterReplId() {
                return "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
            }

            @Override
            public long getMasterReplOffset() {
                return 0;
            }

            @Override
            public String toInfoSection() {
                return infoSection;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Tests — "replication" section
    // -------------------------------------------------------------------------

    @Test
    void execute_withReplicationSection_returnsBulkStringOfInfoSection() {
        // Arrange
        ReplicationInfo info = stubReplicationInfo("master", "role:master");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "replication"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        // Must be a RESP bulk string ($<len>\r\n<body>\r\n)
        assertTrue(encoded.startsWith("$"));
        assertTrue(encoded.contains("role:master"));
    }

    @Test
    void execute_withReplicationSectionUpperCase_returnsBulkString() {
        // Arrange — section arg is case-insensitive per Redis spec
        ReplicationInfo info = stubReplicationInfo("slave", "role:slave");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "REPLICATION"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("role:slave"));
    }

    // -------------------------------------------------------------------------
    // Tests — "all" / no section
    // -------------------------------------------------------------------------

    @Test
    void execute_withNoSection_defaultsToAll_andReturnsInfoSection() {
        // Arrange
        ReplicationInfo info = stubReplicationInfo("master", "role:master");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("role:master"));
    }

    @Test
    void execute_withAllSection_returnsInfoSection() {
        // Arrange
        ReplicationInfo info = stubReplicationInfo("master", "role:master");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "all"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("role:master"));
    }

    // -------------------------------------------------------------------------
    // Tests — unknown section
    // -------------------------------------------------------------------------

    @Test
    void execute_withUnknownSection_returnsEmptyBulkString() {
        // Arrange
        ReplicationInfo info = stubReplicationInfo("master", "role:master");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "server"});

        // Assert — body is empty: "$0\r\n\r\n"
        assertEquals("$0\r\n\r\n", new String(result, StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Tests — using StandaloneReplicationInfo directly
    // -------------------------------------------------------------------------

    @Test
    void execute_withStandaloneReplicationInfo_masterRole_containsRoleInfo() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("master");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "replication"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("role:master"));
    }

    @Test
    void execute_withStandaloneReplicationInfo_slaveRole_containsRoleInfo() {
        // Arrange
        StandaloneReplicationInfo info = new StandaloneReplicationInfo("slave");
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "replication"});

        // Assert
        String encoded = new String(result, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("role:slave"));
    }

    @Test
    void execute_returnsBulkStringFormat_withCorrectLength() {
        // Arrange
        String infoBody = "role:master";
        ReplicationInfo info = stubReplicationInfo("master", infoBody);
        InfoCommandHandler handler = new InfoCommandHandler(info);

        // Act
        byte[] result = handler.execute(new String[]{"INFO", "replication"});

        // Assert — the length prefix must match the body length
        String encoded = new String(result, StandardCharsets.UTF_8);
        String expectedPrefix = "$" + infoBody.length() + "\r\n";
        assertTrue(encoded.startsWith(expectedPrefix),
                "Expected bulk string prefix '" + expectedPrefix + "' but got: " + encoded);
    }
}
