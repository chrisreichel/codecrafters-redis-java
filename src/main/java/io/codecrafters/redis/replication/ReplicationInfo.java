package io.codecrafters.redis.replication;

/**
 * Provides read access to the server's replication metadata.
 */
public interface ReplicationInfo {

    /**
     * Returns the role of this server in the replication topology.
     *
     * @return "master" or "slave"
     */
    String getRole();

    /**
     * Returns a formatted key:value block for embedding in an INFO response.
     *
     * @return the formatted replication section text
     */
    String toInfoSection();
}
