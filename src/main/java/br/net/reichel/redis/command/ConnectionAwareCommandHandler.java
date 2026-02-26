package br.net.reichel.redis.command;

import br.net.reichel.redis.server.ConnectionInfo;

/**
 * Optional extension for commands that need caller connection context.
 */
public interface ConnectionAwareCommandHandler extends CommandHandler {

    /**
     * Executes the command with access to the current connection metadata.
     *
     * @param args command tokens
     * @param connection caller connection metadata
     * @return RESP-encoded response bytes
     */
    byte[] execute(String[] args, ConnectionInfo connection);
}
