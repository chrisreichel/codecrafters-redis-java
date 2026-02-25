package br.net.reichel.redis.replication.impl;

import br.net.reichel.redis.replication.ReplicationInfo;

/**
 * Replication metadata for a standalone (non-replicated) Redis server instance.
 */
public class StandaloneReplicationInfo implements ReplicationInfo {

    private final String role;
    private final String masterReplId;
    private final long masterReplOffset;
    private String masterHost;
    private int masterPort = -1;

    /**
     * Creates replication info with the given role.
     *
     * @param role "master" or "slave"
     */
    public StandaloneReplicationInfo(String role) {
        this.role = role;
        this.masterReplId = "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb";
        this.masterReplOffset = 0;
    }

    /** @return the replication role */
    @Override
    public String getRole() {
        return role;
    }

    @Override
    public String getMasterReplId() {
        return masterReplId;
    }

    @Override
    public long getMasterReplOffset() {
        return masterReplOffset;
    }

    @Override
    public String getMasterHost() {
        return masterHost;
    }

    @Override
    public int getMasterPort() {
        return masterPort;
    }

    @Override
    public void setMaster(String host, int port) {
        this.masterHost = host;
        this.masterPort = port;
    }

    /**
     * Returns the INFO replication section as newline-delimited key:value pairs.
     *
     * @return formatted replication section text
     */
    @Override
    public String toInfoSection() {
        return "role:" + role + "\r\n" +
               "master_replid:" + masterReplId + "\r\n" +
               "master_repl_offset:" + masterReplOffset;
    }
}
