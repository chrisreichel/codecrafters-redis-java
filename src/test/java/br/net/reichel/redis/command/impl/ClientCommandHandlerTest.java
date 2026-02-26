package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.server.ConnectionInfo;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientCommandHandlerTest {

    private final ClientCommandHandler handler = new ClientCommandHandler();
    private final ConnectionInfo connection = new ConnectionInfo(
            42L,
            new InetSocketAddress("127.0.0.1", 12345),
            System.currentTimeMillis()
    );

    @Test
    void execute_id_returnsConnectionIdAsRespInteger() {
        String response = new String(handler.execute(new String[]{"CLIENT", "ID"}, connection), StandardCharsets.UTF_8);
        assertEquals(":42\r\n", response);
    }

    @Test
    void execute_withoutSubcommand_returnsError() {
        String response = new String(handler.execute(new String[]{"CLIENT"}, connection), StandardCharsets.UTF_8);
        assertTrue(response.startsWith("-ERR"));
    }

    @Test
    void execute_unknownSubcommand_returnsError() {
        String response = new String(handler.execute(new String[]{"CLIENT", "FOO"}, connection), StandardCharsets.UTF_8);
        assertTrue(response.startsWith("-ERR"));
    }
}
