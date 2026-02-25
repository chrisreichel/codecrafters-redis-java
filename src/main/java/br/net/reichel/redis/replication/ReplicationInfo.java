package br.net.reichel.redis.replication;

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
     * Returns the master replication ID.
     *
     * @return the replication ID
     */
    String getMasterReplId();

    /**
     * Returns the master replication offset.
     *
     * @return the replication offset
     */
    long getMasterReplOffset();

    /**
     * Returns a formatted key:value block for embedding in an INFO response.
     *
     * @return the formatted replication section text
     */
    String toInfoSection();
}
