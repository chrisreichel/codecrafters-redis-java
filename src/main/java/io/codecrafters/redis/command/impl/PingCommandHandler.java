package io.codecrafters.redis.command.impl;

import io.codecrafters.redis.command.CommandHandler;
import io.codecrafters.redis.protocol.RespEncoder;

/**
 * Handles the Redis PING command.
 */
public class PingCommandHandler implements CommandHandler {

    /**
     * Responds with PONG.
     *
     * @param args the command arguments
     * @return RESP simple string "+PONG\r\n"
     */
    @Override
    public byte[] execute(String[] args) {
        return RespEncoder.simpleString("PONG");
    }
}
