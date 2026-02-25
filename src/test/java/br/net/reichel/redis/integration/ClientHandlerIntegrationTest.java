package br.net.reichel.redis.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link br.net.reichel.redis.server.ClientHandler}.
 * Each test gets a fresh server via {@link RedisServerFixture}.
 */
class ClientHandlerIntegrationTest {

    private RedisServerFixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = new RedisServerFixture();
    }

    @AfterEach
    void tearDown() throws Exception {
        fixture.stop();
    }

    @Test
    void dispatch_ping_returnsPong() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String response = client.send("PING");
            assertEquals("+PONG\r\n", response);
        }
    }

    @Test
    void dispatch_unknownCommand_returnsError() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String response = client.send("FOOBAR");
            assertTrue(response.startsWith("-"));
            assertTrue(response.contains("FOOBAR"));
        }
    }

    @Test
    void dispatch_multi_setsTransactionActive() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String response = client.send("MULTI");
            assertEquals("+OK\r\n", response);
        }
    }

    @Test
    void dispatch_commandInTransaction_returnsQueued() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("MULTI");
            String response = client.send("SET", "k", "v");
            assertEquals("+QUEUED\r\n", response);
        }
    }

    @Test
    void dispatch_exec_returnsArrayOfResults() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("MULTI");
            client.send("SET", "txkey", "txval");
            client.send("GET", "txkey");
            String response = client.send("EXEC");
            assertTrue(response.startsWith("*2\r\n"));
            assertTrue(response.contains("+OK\r\n"));
            assertTrue(response.contains("txval"));
        }
    }

    @Test
    void dispatch_exec_withoutMulti_returnsError() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String response = client.send("EXEC");
            assertTrue(response.startsWith("-"));
            assertTrue(response.contains("EXEC without MULTI"));
        }
    }

    @Test
    void dispatch_discard_afterMulti_returnsOk() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("MULTI");
            String response = client.send("DISCARD");
            assertEquals("+OK\r\n", response);
        }
    }

    @Test
    void dispatch_discard_withoutMulti_returnsError() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String response = client.send("DISCARD");
            assertTrue(response.startsWith("-"));
            assertTrue(response.contains("DISCARD without MULTI"));
        }
    }

    @Test
    void connectionClose_cleanEof() throws IOException {
        // Close socket mid-session; server thread should exit cleanly
        RespTestClient client = fixture.newClient();
        client.send("PING");
        client.close();
        // Give the server thread a moment to clean up — no assertion needed, just no exception
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void dispatch_clientId_returnsStableIntegerPerConnection() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            String first = client.send("CLIENT", "ID");
            String second = client.send("CLIENT", "ID");
            assertTrue(first.startsWith(":"));
            assertEquals(first, second);
        }
    }

    @Test
    void dispatch_clientId_returnsDifferentIdsForDifferentConnections() throws IOException {
        String firstId;
        try (RespTestClient client1 = fixture.newClient()) {
            firstId = client1.send("CLIENT", "ID");
        }

        try (RespTestClient client2 = fixture.newClient()) {
            String secondId = client2.send("CLIENT", "ID");
            assertNotEquals(firstId, secondId);
        }
    }
}
