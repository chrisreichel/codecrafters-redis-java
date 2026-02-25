package br.net.reichel.redis.integration;

import br.net.reichel.redis.Main;
import br.net.reichel.redis.command.CommandHandler;
import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.store.impl.InMemoryDataStore;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link CommandRegistry} and {@link Main#buildRegistry}.
 * These tests run in-process with no socket required.
 */
class CommandRegistryIntegrationTest {

    @Test
    void buildRegistry_registersAllSeventeenCommands() {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), new br.net.reichel.redis.replication.impl.StandaloneReplicationInfo("master"));

        String[] commands = {"PING", "ECHO", "SET", "GET", "INCR", "TYPE",
                "RPUSH", "LPUSH", "LLEN", "LRANGE", "LPOP", "BLPOP",
                "XADD", "XRANGE", "XREAD", "INFO", "CLIENT"};

        for (String cmd : commands) {
            assertTrue(registry.resolve(cmd).isPresent(), "Expected handler for: " + cmd);
        }
    }

    @Test
    void resolve_isCaseInsensitive() {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), new br.net.reichel.redis.replication.impl.StandaloneReplicationInfo("master"));

        Optional<CommandHandler> lower = registry.resolve("ping");
        Optional<CommandHandler> upper = registry.resolve("PING");

        assertTrue(lower.isPresent());
        assertTrue(upper.isPresent());
        assertSame(lower.get(), upper.get());
    }

    @Test
    void resolve_unknownCommand_returnsEmpty() {
        CommandRegistry registry = Main.buildRegistry(new InMemoryDataStore(), new br.net.reichel.redis.replication.impl.StandaloneReplicationInfo("master"));

        Optional<CommandHandler> result = registry.resolve("FOOBAR");

        assertTrue(result.isEmpty());
    }

    @Test
    void register_overwritesHandler() {
        CommandRegistry registry = new CommandRegistry();
        CommandHandler first = args -> new byte[0];
        CommandHandler second = args -> new byte[]{1};

        registry.register("CMD", first);
        registry.register("CMD", second);

        assertSame(second, registry.resolve("CMD").get());
    }
}
