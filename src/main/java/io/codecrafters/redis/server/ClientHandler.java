package io.codecrafters.redis.server;

import io.codecrafters.redis.command.CommandRegistry;
import io.codecrafters.redis.protocol.RespEncoder;
import io.codecrafters.redis.protocol.RespParser;
import io.codecrafters.redis.transaction.TransactionContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Handles a single client connection, dispatching commands to the registry
 * and managing per-connection transaction state (MULTI/EXEC/DISCARD).
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CommandRegistry registry;

    /**
     * Creates a handler for the given client socket.
     *
     * @param socket   the client socket
     * @param registry the command registry to dispatch commands to
     */
    public ClientHandler(Socket socket, CommandRegistry registry) {
        this.socket = socket;
        this.registry = registry;
    }

    /**
     * Reads RESP commands from the client and dispatches them until the connection closes.
     */
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            var out = socket.getOutputStream();
            RespParser parser = new RespParser(reader);
            TransactionContext tx = new TransactionContext();

            Optional<String[]> commandOpt;
            while ((commandOpt = parser.readCommand()).isPresent()) {
                String[] args = commandOpt.get();
                String command = args[0].toUpperCase();

                if (tx.isActive() && !command.equals("EXEC") && !command.equals("DISCARD")) {
                    tx.enqueue(args);
                    out.write(RespEncoder.simpleString("QUEUED"));
                    continue;
                }

                byte[] response = switch (command) {
                    case "MULTI"   -> handleMulti(tx);
                    case "EXEC"    -> handleExec(tx);
                    case "DISCARD" -> handleDiscard(tx);
                    default -> registry.resolve(command)
                            .map(h -> h.execute(args))
                            .orElse(RespEncoder.error("ERR unknown command '" + command + "'"));
                };

                out.write(response);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }

    private byte[] handleMulti(TransactionContext tx) {
        tx.begin();
        return RespEncoder.simpleString("OK");
    }

    private byte[] handleExec(TransactionContext tx) {
        if (!tx.isActive()) {
            return RespEncoder.error("ERR EXEC without MULTI");
        }
        List<String[]> queued = tx.commitAndClear();
        StringBuilder sb = new StringBuilder("*").append(queued.size()).append("\r\n");
        for (String[] cmd : queued) {
            byte[] result = registry.resolve(cmd[0].toUpperCase())
                    .map(h -> h.execute(cmd))
                    .orElse(RespEncoder.error("ERR unknown command"));
            sb.append(new String(result, StandardCharsets.UTF_8));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleDiscard(TransactionContext tx) {
        if (!tx.isActive()) {
            return RespEncoder.error("ERR DISCARD without MULTI");
        }
        tx.rollback();
        return RespEncoder.simpleString("OK");
    }
}
