package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;

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
