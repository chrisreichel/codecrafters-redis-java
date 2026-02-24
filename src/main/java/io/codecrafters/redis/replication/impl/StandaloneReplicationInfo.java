package io.codecrafters.redis.replication.impl;

import io.codecrafters.redis.replication.ReplicationInfo;

/**
 * Replication metadata for a standalone (non-replicated) Redis server instance.
 */
public class StandaloneReplicationInfo implements ReplicationInfo {

    private final String role;

    /**
     * Creates replication info with the given role.
     *
     * @param role "master" or "slave"
     */
    public StandaloneReplicationInfo(String role) {
        this.role = role;
    }

    /** @return the replication role */
    @Override
    public String getRole() {
        return role;
    }

    /**
     * Returns the INFO replication section as newline-delimited key:value pairs.
     *
     * @return formatted replication section text
     */
    @Override
    public String toInfoSection() {
        return "role:" + role;
    }
}
