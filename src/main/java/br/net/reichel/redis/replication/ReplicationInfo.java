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
     * Returns the master host if this server is a replica.
     *
     * @return the master host, or null if this is a master
     */
    String getMasterHost();

    /**
     * Returns the master port if this server is a replica.
     *
     * @return the master port, or -1 if this is a master
     */
    int getMasterPort();

    /**
     * Sets the master connection details.
     *
     * @param host the master host
     * @param port the master port
     */
    void setMaster(String host, int port);

    /**
     * Returns a formatted key:value block for embedding in an INFO response.
     *
     * @return the formatted replication section text
     */
    String toInfoSection();
}
