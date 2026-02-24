package io.codecrafters.redis.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Optional;

/**
 * Parses RESP (REdis Serialization Protocol) array frames from a buffered reader.
 */
public class RespParser {

    private final BufferedReader reader;

    /**
     * Creates a parser backed by the given reader.
     *
     * @param reader the buffered reader to read from
     */
    public RespParser(BufferedReader reader) {
        this.reader = reader;
    }

    /**
     * Reads the next RESP array frame as a token array.
     *
     * @return the array of command tokens, or empty if the connection was closed
     * @throws IOException if a read error occurs
     */
    public Optional<String[]> readCommand() throws IOException {
        String line = reader.readLine();
        if (line == null || !line.startsWith("*")) {
            return Optional.empty();
        }
        int numElements = Integer.parseInt(line.substring(1));
        String[] elements = new String[numElements];
        for (int i = 0; i < numElements; i++) {
            reader.readLine(); // skip $<length>
            elements[i] = reader.readLine();
        }
        return Optional.of(elements);
    }
}
