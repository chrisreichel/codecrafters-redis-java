package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicationIntegrationTest {

    private RedisServer startServer(String role) throws IOException, InterruptedException {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), new br.net.reichel.redis.replication.impl.StandaloneReplicationInfo(role));
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
            assertTrue(response.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"), "Missing master_replid");
            assertTrue(response.contains("master_repl_offset:0"), "Missing master_repl_offset");
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
            assertTrue(response.contains("master_replid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"), "Missing master_replid");
            assertTrue(response.contains("master_repl_offset:0"), "Missing master_repl_offset");
        } finally {
            server.stop();
        }
    }
}
