package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicationIntegrationTest {

    private RedisServer startServer(String role) throws IOException, InterruptedException {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), role);
        RedisServer server = new RedisServer(0, registry);
        Thread t = Thread.startVirtualThread(() -> {
            try {
                server.start();
            } catch (IOException ignored) {
            }
        });
        server.awaitReady();
        return server;
    }

    @Test
    void infoReplication_returnsMasterRole_byDefault() throws Exception {
        RedisServer server = startServer("master");
        try (RespTestClient client = new RespTestClient("localhost", server.getLocalPort())) {
            String response = client.send("INFO", "replication");
            assertTrue(response.contains("role:master"), "Expected role:master in response but got: " + response);
        } finally {
            server.stop();
        }
    }

    @Test
    void infoReplication_returnsSlaveRole_whenConfiguredAsSlave() throws Exception {
        RedisServer server = startServer("slave");
        try (RespTestClient client = new RespTestClient("localhost", server.getLocalPort())) {
            String response = client.send("INFO", "replication");
            assertTrue(response.contains("role:slave"), "Expected role:slave in response but got: " + response);
        } finally {
            server.stop();
        }
    }
}
