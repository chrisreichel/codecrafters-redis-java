package io.codecrafters.redis;

import io.codecrafters.redis.command.CommandRegistry;
import io.codecrafters.redis.command.impl.*;
import io.codecrafters.redis.server.RedisServer;
import io.codecrafters.redis.store.DataStore;
import io.codecrafters.redis.store.impl.InMemoryDataStore;

import java.io.IOException;

/**
 * Entry point for the Redis server.
 */
public class Main {

    /**
     * Starts the Redis server on port 6379.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        int port = 6379;
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--port")) {
                port = Integer.parseInt(args[i + 1]);
            }
        }

        DataStore store = new InMemoryDataStore();
        CommandRegistry registry = buildRegistry(store);

        try {
            new RedisServer(port, registry).start();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static CommandRegistry buildRegistry(DataStore store) {
        CommandRegistry registry = new CommandRegistry();
        registry.register("PING",   new PingCommandHandler());
        registry.register("ECHO",   new EchoCommandHandler());
        registry.register("SET",    new SetCommandHandler(store));
        registry.register("GET",    new GetCommandHandler(store));
        registry.register("INCR",   new IncrCommandHandler(store));
        registry.register("TYPE",   new TypeCommandHandler(store));
        registry.register("RPUSH",  new RpushCommandHandler(store));
        registry.register("LPUSH",  new LpushCommandHandler(store));
        registry.register("LLEN",   new LlenCommandHandler(store));
        registry.register("LRANGE", new LrangeCommandHandler(store));
        registry.register("LPOP",   new LpopCommandHandler(store));
        registry.register("BLPOP",  new BlpopCommandHandler(store));
        registry.register("XADD",   new XaddCommandHandler(store));
        registry.register("XRANGE", new XrangeCommandHandler(store));
        registry.register("XREAD",  new XreadCommandHandler(store));
        return registry;
    }
}
