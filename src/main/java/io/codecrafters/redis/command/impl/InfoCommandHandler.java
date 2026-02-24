package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.replication.ReplicationInfo;

/**
 * Handles the Redis INFO command.
 */
public class InfoCommandHandler implements CommandHandler {

    private final ReplicationInfo replicationInfo;

    /**
     * Creates a handler backed by the given replication info provider.
     *
     * @param replicationInfo the replication metadata source
     */
    public InfoCommandHandler(ReplicationInfo replicationInfo) {
        this.replicationInfo = replicationInfo;
    }

    /**
     * Executes the INFO command; args[1] (optional) is the section name.
     *
     * @param args the command arguments
     * @return a RESP bulk string containing the requested section
     */
    @Override
    public byte[] execute(String[] args) {
        String section = args.length > 1 ? args[1].toLowerCase() : "all";
        String body = switch (section) {
            case "replication", "all" -> replicationInfo.toInfoSection();
            default -> "";
        };
        return RespEncoder.bulkString(body);
    }
}
