package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RedisServer}.
 */
class RedisServerIntegrationTest {

    private RedisServer buildServer() {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), new br.net.reichel.redis.replication.impl.StandaloneReplicationInfo("master"));
        return new RedisServer(0, registry);
    }

    @Test
    void start_acceptsConnections_afterBind() throws Exception {
        RedisServer server = buildServer();
        Thread t = Thread.startVirtualThread(() -> {
            try { server.start(); } catch (IOException ignored) {}
        });
        server.awaitReady();

        try (RespTestClient client = new RespTestClient("localhost", server.getLocalPort())) {
            assertEquals("+PONG\r\n", client.send("PING"));
        } finally {
            server.stop();
            t.join(2000);
        }
    }

    @Test
    void start_handlesConcurrentClients() throws Exception {
        RedisServer server = buildServer();
        Thread t = Thread.startVirtualThread(() -> {
            try { server.start(); } catch (IOException ignored) {}
        });
        server.awaitReady();

        int count = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(count);
        List<String> responses = new ArrayList<>();

        try {
            for (int i = 0; i < count; i++) {
                Thread.startVirtualThread(() -> {
                    try {
                        startLatch.await();
                        try (RespTestClient client = new RespTestClient("localhost", server.getLocalPort())) {
                            synchronized (responses) {
                                responses.add(client.send("PING"));
                            }
                        }
                    } catch (Exception e) {
                        synchronized (responses) {
                            responses.add("ERROR: " + e.getMessage());
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            doneLatch.await();

            assertEquals(count, responses.size());
            for (String r : responses) {
                assertEquals("+PONG\r\n", r);
            }
        } finally {
            server.stop();
            t.join(2000);
        }
    }

    @Test
    void stop_causesStartToReturn() throws Exception {
        RedisServer server = buildServer();
        Thread t = Thread.startVirtualThread(() -> {
            try { server.start(); } catch (IOException ignored) {}
        });
        server.awaitReady();
        server.stop();
        t.join(2000);
        assertFalse(t.isAlive(), "Server thread should have terminated after stop()");
    }

    @Test
    void stop_allowsPortReuse() throws Exception {
        RedisServer server1 = buildServer();
        Thread t1 = Thread.startVirtualThread(() -> {
            try { server1.start(); } catch (IOException ignored) {}
        });
        server1.awaitReady();
        int port = server1.getLocalPort();
        server1.stop();
        t1.join(2000);

        // Rebind the exact same port immediately
        try (ServerSocket check = new ServerSocket(port)) {
            check.setReuseAddress(true);
            // If we get here without exception, the port was released
            assertTrue(check.isBound());
        }
    }

    @Test
    void activeConnectionCount_tracksOpenAndClosedClients() throws Exception {
        RedisServer server = buildServer();
        Thread t = Thread.startVirtualThread(() -> {
            try { server.start(); } catch (IOException ignored) {}
        });
        server.awaitReady();

        RespTestClient client1 = null;
        RespTestClient client2 = null;
        try {
            assertEquals(0, server.getActiveConnectionCount());

            client1 = new RespTestClient("localhost", server.getLocalPort());
            client1.send("PING");
            assertEquals(1, server.getActiveConnectionCount());

            client2 = new RespTestClient("localhost", server.getLocalPort());
            client2.send("PING");
            assertEquals(2, server.getActiveConnectionCount());

            client1.close();
            Thread.sleep(100);
            assertEquals(1, server.getActiveConnectionCount());
        } finally {
            if (client2 != null) {
                client2.close();
                Thread.sleep(100);
                assertEquals(0, server.getActiveConnectionCount());
            }
            server.stop();
            t.join(2000);
        }
    }
}
