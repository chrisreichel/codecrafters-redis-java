package br.net.reichel.redis.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the XREAD command (blocking and non-blocking).
 * Each test uses a fresh server fixture.
 */
class XreadIntegrationTest {

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
    void xread_nonBlock_returnsEntriesAfterCursor() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("XADD", "mystream", "*", "field1", "value1");
            String response = client.send("XREAD", "COUNT", "10", "STREAMS", "mystream", "0-0");
            assertNotNull(response);
            assertTrue(response.contains("mystream"));
            assertTrue(response.contains("field1"));
        }
    }

    @Test
    void xread_block_returnsImmediately_whenNewDataExists() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("XADD", "existstream", "*", "k", "v");
            String response = client.send("XREAD", "BLOCK", "0", "STREAMS", "existstream", "0-0");
            assertNotNull(response);
            assertTrue(response.contains("existstream"));
        }
    }

    @Test
    void xread_block_waitsForXaddFromSecondClient() throws Exception {
        assertTimeout(ofSeconds(5), () -> {
            try (RespTestClient client1 = fixture.newClient()) {
                client1.sendFrame("XREAD", "BLOCK", "5000", "STREAMS", "events", "$");

                Thread producer = Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(200);
                        try (RespTestClient client2 = fixture.newClient()) {
                            client2.send("XADD", "events", "*", "field", "value");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                producer.join(3000);
                String response = client1.readResponse();
                assertNotNull(response);
                assertTrue(response.contains("events"));
                assertTrue(response.contains("field"));
            }
        });
    }

    @Test
    void xread_block_returnsNullArray_onTimeout() {
        assertTimeout(ofSeconds(3), () -> {
            try (RespTestClient client = fixture.newClient()) {
                String response = client.send("XREAD", "BLOCK", "1000", "STREAMS", "emptystream", "$");
                assertEquals("*-1\r\n", response);
            }
        });
    }
}
