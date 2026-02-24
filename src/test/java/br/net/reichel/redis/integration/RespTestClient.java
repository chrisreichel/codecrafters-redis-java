package br.net.reichel.redis.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Simple RESP protocol client for integration tests.
 */
public class RespTestClient implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader reader;
    private final OutputStream out;

    /**
     * Connects to a Redis server at the given host and port.
     *
     * @param host the server hostname
     * @param port the server port
     * @throws IOException if the connection fails
     */
    public RespTestClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = socket.getOutputStream();
    }

    /**
     * Sends a RESP array command and reads the full response.
     *
     * @param args command tokens
     * @return the raw RESP response string
     * @throws IOException if an I/O error occurs
     */
    public String send(String... args) throws IOException {
        sendFrame(args);
        return readResponse();
    }

    /**
     * Writes a RESP array frame without reading the response.
     * Useful before blocking commands where you want to read the response later.
     *
     * @param args command tokens
     * @throws IOException if an I/O error occurs
     */
    public void sendFrame(String... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.length).append("\r\n");
        for (String arg : args) {
            sb.append("$").append(arg.getBytes(StandardCharsets.UTF_8).length).append("\r\n");
            sb.append(arg).append("\r\n");
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * Reads one complete RESP frame from the server.
     * Handles simple strings (+), errors (-), integers (:), bulk strings ($), and arrays (*) recursively.
     *
     * @return the raw RESP response string
     * @throws IOException if an I/O error occurs
     */
    public String readResponse() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }

        char type = line.charAt(0);
        String data = line.substring(1);

        return switch (type) {
            case '+' -> "+" + data + "\r\n";
            case '-' -> "-" + data + "\r\n";
            case ':' -> ":" + data + "\r\n";
            case '$' -> {
                int length = Integer.parseInt(data);
                if (length == -1) {
                    yield "$-1\r\n";
                }
                String bulk = reader.readLine();
                yield "$" + length + "\r\n" + bulk + "\r\n";
            }
            case '*' -> {
                int count = Integer.parseInt(data);
                if (count == -1) {
                    yield "*-1\r\n";
                }
                StringBuilder sb = new StringBuilder("*").append(count).append("\r\n");
                for (int i = 0; i < count; i++) {
                    sb.append(readResponse());
                }
                yield sb.toString();
            }
            default -> line + "\r\n";
        };
    }

    /**
     * Closes the underlying socket.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }
}
