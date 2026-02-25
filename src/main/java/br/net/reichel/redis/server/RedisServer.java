package br.net.reichel.redis.server;

import br.net.reichel.redis.command.CommandRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CountDownLatch;

/**
 * TCP server that accepts client connections and spawns a virtual thread for each.
 */
public class RedisServer {

    private final int port;
    private final CommandRegistry registry;
    private final ConnectionRegistry connectionRegistry = new ConnectionRegistry();
    private final AtomicLong nextConnectionId = new AtomicLong(1);
    private volatile ServerSocket serverSocket;
    private final CountDownLatch readyLatch = new CountDownLatch(1);

    /**
     * Creates a server that will listen on the given port. Use port 0 for an ephemeral port.
     *
     * @param port     the port to bind to (0 for ephemeral)
     * @param registry the command registry for dispatching client commands
     */
    public RedisServer(int port, CommandRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    /**
     * Starts the server and blocks, accepting client connections until stopped.
     *
     * @throws IOException if the server socket cannot be opened
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        readyLatch.countDown();
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                long connectionId = nextConnectionId.getAndIncrement();
                ConnectionInfo connectionInfo = new ConnectionInfo(
                        connectionId,
                        clientSocket.getRemoteSocketAddress(),
                        System.currentTimeMillis()
                );
                connectionRegistry.register(connectionInfo);
                Thread.startVirtualThread(new ClientHandler(clientSocket, registry, connectionInfo, connectionRegistry));
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) throw e;
            // expected: accept() throws SocketException when stop() closes the socket
        } finally {
            serverSocket.close();
        }
    }

    /**
     * Stops the server by closing its socket. {@link #start()} will return after this call.
     *
     * @throws IOException if the socket cannot be closed
     */
    public void stop() throws IOException {
        if (serverSocket != null) serverSocket.close();
    }

    /**
     * Returns the bound local port. Valid after {@link #start()} has been called.
     *
     * @return the local port, or -1 if not yet bound
     */
    public int getLocalPort() {
        return (serverSocket != null) ? serverSocket.getLocalPort() : -1;
    }

    /**
     * Returns active connection count.
     *
     * @return number of active client connections
     */
    public int getActiveConnectionCount() {
        return connectionRegistry.size();
    }

    /**
     * Blocks until the server socket is bound and ready to accept connections.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void awaitReady() throws InterruptedException {
        readyLatch.await();
    }
}
