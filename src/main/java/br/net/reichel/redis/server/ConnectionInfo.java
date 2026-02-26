package br.net.reichel.redis.server;

import java.net.SocketAddress;

/**
 * Metadata for one active client connection.
 *
 * @param id          unique server-assigned connection id
 * @param remoteAddr  client remote socket address
 * @param connectedAt epoch millis when accepted
 */
public record ConnectionInfo(long id, SocketAddress remoteAddr, long connectedAt) {
}
