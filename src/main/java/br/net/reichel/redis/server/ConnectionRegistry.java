package br.net.reichel.redis.server;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks active client connections by id.
 */
public class ConnectionRegistry {

    private final ConcurrentMap<Long, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();

    /**
     * Registers an active connection.
     *
     * @param info connection metadata
     */
    public void register(ConnectionInfo info) {
        activeConnections.put(info.id(), info);
    }

    /**
     * Unregisters a connection by id.
     *
     * @param id connection id
     */
    public void unregister(long id) {
        activeConnections.remove(id);
    }

    /**
     * Looks up a connection by id.
     *
     * @param id connection id
     * @return connection metadata if present
     */
    public Optional<ConnectionInfo> find(long id) {
        return Optional.ofNullable(activeConnections.get(id));
    }

    /**
     * Returns the number of active connections.
     *
     * @return active connection count
     */
    public int size() {
        return activeConnections.size();
    }
}
