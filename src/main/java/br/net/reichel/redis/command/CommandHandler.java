package br.net.reichel.redis.command;

/**
 * Handles a single Redis command and returns a RESP-encoded response.
 * Implementations may block the calling thread for blocking commands (e.g., BLPOP, XREAD BLOCK).
 */
public interface CommandHandler {

    /**
     * Executes the command with the given arguments.
     *
     * @param args the full tokenized command array, where args[0] is the command name
     * @return RESP-encoded response bytes
     */
    byte[] execute(String[] args);
}
