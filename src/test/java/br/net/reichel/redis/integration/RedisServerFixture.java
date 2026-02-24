package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.impl.InMemoryDataStore;

import java.io.IOException;

/**
 * Manages a live RedisServer for integration tests.
 * Creates a server on an ephemeral port (port 0), starts it on a virtual thread,
 * and waits until it is ready to accept connections.
 */
public class RedisServerFixture {

    private final RedisServer server;
    private final Thread serverThread;

    /**
     * Creates and starts a Redis server on an ephemeral port.
     *
     * @throws IOException          if the server socket cannot be opened
     * @throws InterruptedException if the calling thread is interrupted while waiting for the server to be ready
     */
    public RedisServerFixture() throws IOException, InterruptedException {
        InMemoryDataStore store = new InMemoryDataStore();
        CommandRegistry registry = Main.buildRegistry(store);
        this.server = new RedisServer(0, registry);

        serverThread = Thread.startVirtualThread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // expected when stop() is called
            }
        });

        server.awaitReady();
    }

    /**
     * Returns the bound local port of the running server.
     *
     * @return the local port
     */
    public int getPort() {
        return server.getLocalPort();
    }

    /**
     * Opens a new client connection to the server.
     *
     * @return a connected {@link RespTestClient}
     * @throws IOException if the connection fails
     */
    public RespTestClient newClient() throws IOException {
        return new RespTestClient("localhost", getPort());
    }

    /**
     * Stops the server and waits for the server thread to terminate.
     *
     * @throws IOException          if the socket cannot be closed
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void stop() throws IOException, InterruptedException {
        server.stop();
        serverThread.join(2000);
    }
}
