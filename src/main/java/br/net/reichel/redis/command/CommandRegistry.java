package br.net.reichel.redis.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dispatches incoming Redis commands to their registered {@link CommandHandler} instances.
 */
public class CommandRegistry {

    private final Map<String, CommandHandler> handlers = new HashMap<>();

    /**
     * Registers a handler for the given command name (case-insensitive).
     *
     * @param commandName the Redis command name
     * @param handler     the handler to invoke
     */
    public void register(String commandName, CommandHandler handler) {
        handlers.put(commandName.toUpperCase(), handler);
    }

    /**
     * Resolves the handler for the given command name.
     *
     * @param commandName the command name token from the client
     * @return an Optional containing the handler, or empty if unrecognized
     */
    public Optional<CommandHandler> resolve(String commandName) {
        return Optional.ofNullable(handlers.get(commandName.toUpperCase()));
    }
}
