package br.net.reichel.redis.integration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Simple RESP protocol client for integration tests.
 */
public class RespTestClient implements AutoCloseable {

    private final Socket socket;
    private final InputStream in;
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
        this.in = socket.getInputStream();
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
        String line = readLine();
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
                byte[] bytes = new byte[length];
                int read = 0;
                while (read < length) {
                    int r = in.read(bytes, read, length - read);
                    if (r == -1) throw new IOException("Unexpected EOF while reading bulk string body");
                    read += r;
                }
                // Read the trailing \r\n
                if (in.read() != '\r' || in.read() != '\n') {
                    throw new IOException("Expected \\r\\n after bulk string body");
                }
                String bulk = new String(bytes, StandardCharsets.UTF_8);
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

    private String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next == '\n') {
                    break;
                }
                baos.write(b);
                baos.write(next);
            } else {
                baos.write(b);
            }
        }
        if (b == -1 && baos.size() == 0) {
            return null;
        }
        return baos.toString(StandardCharsets.UTF_8);
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
