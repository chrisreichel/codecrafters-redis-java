package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.replication.ReplicationInfo;
import br.net.reichel.redis.replication.impl.StandaloneReplicationInfo;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandshakeIntegrationTest {

    @Test
    void replica_sendsPingToMasterOnInit() throws Exception {
        try (ServerSocket mockMaster = new ServerSocket(0)) {
            int masterPort = mockMaster.getLocalPort();

            CompletableFuture<String> receivedCommand = new CompletableFuture<>();

            Thread masterThread = Thread.startVirtualThread(() -> {
                try (Socket client = mockMaster.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                    // We expect *1\r\n$4\r\nPING\r\n
                    StringBuilder sb = new StringBuilder();
                    sb.append(reader.readLine()).append("\r\n"); // *1
                    sb.append(reader.readLine()).append("\r\n"); // $4
                    sb.append(reader.readLine()).append("\r\n"); // PING
                    receivedCommand.complete(sb.toString());
                } catch (IOException e) {
                    receivedCommand.completeExceptionally(e);
                }
            });

            // Start replica
            int replicaPort = 0; // ephemeral
            ReplicationInfo replicationInfo = new StandaloneReplicationInfo("slave");
            replicationInfo.setMaster("localhost", masterPort);
            CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), replicationInfo);
            RedisServer replicaServer = new RedisServer(replicaPort, registry);

            Thread replicaThread = Thread.startVirtualThread(() -> {
                try {
                    br.net.reichel.redis.replication.ReplicationService replicationService =
                            new br.net.reichel.redis.replication.impl.ReplicationServiceImpl(replicationInfo);
                    replicationService.initiateHandshake();
                    replicaServer.start();
                } catch (IOException ignored) {}
            });

            try {
                String command = receivedCommand.get(5, TimeUnit.SECONDS);
                assertEquals("*1\r\n$4\r\nPING\r\n", command);
            } finally {
                replicaServer.stop();
            }
        }
    }
}
