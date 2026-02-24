package br.net.reichel.redis.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the BLPOP command.
 * Each test uses a fresh server fixture.
 */
class BlpopIntegrationTest {

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
    void blpop_returnsImmediately_whenListHasData() throws IOException {
        try (RespTestClient client = fixture.newClient()) {
            client.send("RPUSH", "mylist", "item1");
            String response = client.send("BLPOP", "mylist", "0");
            assertTrue(response.contains("mylist"));
            assertTrue(response.contains("item1"));
        }
    }

    @Test
    void blpop_blocks_untilPushFromSecondClient() throws Exception {
        assertTimeout(ofSeconds(5), () -> {
            try (RespTestClient client1 = fixture.newClient()) {
                client1.sendFrame("BLPOP", "queue", "5");

                Thread pusher = Thread.startVirtualThread(() -> {
                    try {
                        Thread.sleep(200);
                        try (RespTestClient client2 = fixture.newClient()) {
                            client2.send("LPUSH", "queue", "job1");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                pusher.join(3000);
                String response = client1.readResponse();
                assertNotNull(response);
                assertTrue(response.contains("queue"));
                assertTrue(response.contains("job1"));
            }
        });
    }

    @Test
    void blpop_returnsNullArray_onTimeout() {
        assertTimeout(ofSeconds(3), () -> {
            try (RespTestClient client = fixture.newClient()) {
                String response = client.send("BLPOP", "emptylist", "1");
                assertEquals("*-1\r\n", response);
            }
        });
    }
}
