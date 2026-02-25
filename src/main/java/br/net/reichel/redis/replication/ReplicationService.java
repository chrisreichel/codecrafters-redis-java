package br.net.reichel.redis.replication;

import java.io.IOException;

/**
 * Service responsible for managing replication-related actions,
 * such as the handshake between a replica and its master.
 */
public interface ReplicationService {

    /**
     * Initiates the replication handshake with the configured master.
     * This involves sending PING, REPLCONF, and PSYNC commands.
     *
     * @throws IOException if an I/O error occurs during the handshake
     */
    void initiateHandshake() throws IOException;
}
