package io.codecrafters.redis.server;

import io.codecrafters.redis.command.CommandRegistry;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP server that accepts client connections and spawns a virtual thread for each.
 */
public class RedisServer {

    private final int port;
    private final CommandRegistry registry;

    /**
     * Creates a server that will listen on the given port.
     *
     * @param port     the port to bind to
     * @param registry the command registry for dispatching client commands
     */
    public RedisServer(int port, CommandRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    /**
     * Starts the server and blocks indefinitely, accepting client connections.
     *
     * @throws IOException if the server socket cannot be opened
     */
    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(new ClientHandler(clientSocket, registry));
            }
        }
    }
}
