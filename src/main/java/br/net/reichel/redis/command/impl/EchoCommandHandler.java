package br.net.reichel.redis.command.impl;

import br.net.reichel.redis.command.CommandHandler;
import br.net.reichel.redis.protocol.RespEncoder;

/**
 * Handles the Redis ECHO command.
 */
public class EchoCommandHandler implements CommandHandler {

    /**
     * Echoes the argument back as a bulk string.
     *
     * @param args the command arguments; args[1] is the message to echo
     * @return RESP bulk string of args[1]
     */
    @Override
    public byte[] execute(String[] args) {
        return RespEncoder.bulkString(args[1]);
    }
}
