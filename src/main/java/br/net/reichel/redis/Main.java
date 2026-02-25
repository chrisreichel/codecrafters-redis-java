package br.net.reichel.redis;

import br.net.reichel.redis.command.CommandRegistry;
import br.net.reichel.redis.command.impl.*;
import br.net.reichel.redis.replication.ReplicationInfo;
import br.net.reichel.redis.replication.impl.StandaloneReplicationInfo;
import br.net.reichel.redis.server.RedisServer;
import br.net.reichel.redis.store.DataStore;
import br.net.reichel.redis.store.impl.InMemoryDataStore;

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
        String role = "master";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--replicaof") && i + 1 < args.length) {
                role = "slave";
                i++; // Skip the master host/port argument
            }
        }

        DataStore store = new InMemoryDataStore();
        CommandRegistry registry = buildRegistry(store, role);

        try {
            new RedisServer(port, registry).start();
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    public static CommandRegistry buildRegistry(DataStore store, String role) {
        ReplicationInfo replicationInfo = new StandaloneReplicationInfo(role);
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
        registry.register("INFO",   new InfoCommandHandler(replicationInfo));
        return registry;
    }
}
