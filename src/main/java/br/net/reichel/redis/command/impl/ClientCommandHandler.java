package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.command.ConnectionAwareCommandHandler;
import br.net.reichel.redis.protocol.RespEncoder;
import br.net.reichel.redis.server.ConnectionInfo;

/**
 * Handles CLIENT subcommands supported by this server.
 */
public class ClientCommandHandler implements ConnectionAwareCommandHandler {

    @Override
    public byte[] execute(String[] args) {
        return RespEncoder.error("ERR CLIENT requires connection context");
    }

    @Override
    public byte[] execute(String[] args, ConnectionInfo connection) {
        if (args.length < 2) {
            return RespEncoder.error("ERR wrong number of arguments for 'client' command");
        }

        String subcommand = args[1].toUpperCase();
        return switch (subcommand) {
            case "ID" -> RespEncoder.integer(connection.id());
            default -> RespEncoder.error("ERR unsupported CLIENT subcommand '" + subcommand + "'");
        };
    }
}
