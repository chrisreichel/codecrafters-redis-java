package br.net.reichel.redis.replication.impl;

import br.net.reichel.redis.replication.ReplicationInfo;
import br.net.reichel.redis.replication.ReplicationService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of {@link ReplicationService}.
 */
public class ReplicationServiceImpl implements ReplicationService {

    private final ReplicationInfo replicationInfo;

    /**
     * Creates a new service instance.
     *
     * @param replicationInfo the replication metadata provider
     */
    public ReplicationServiceImpl(ReplicationInfo replicationInfo) {
        this.replicationInfo = replicationInfo;
    }

    @Override
    public void initiateHandshake() throws IOException {
        String host = replicationInfo.getMasterHost();
        int port = replicationInfo.getMasterPort();

        if (host == null || port == -1) {
            return;
        }

        try (Socket socket = new Socket(host, port)) {
            OutputStream out = socket.getOutputStream();
            // Send PING as RESP array: *1\r\n$4\r\nPING\r\n
            String pingCommand = "*1\r\n$4\r\nPING\r\n";
            out.write(pingCommand.getBytes(StandardCharsets.UTF_8));
            out.flush();

            // Note: For this stage, we just need to send it.
            // Future stages will handle the response and subsequent commands.
        }
    }
}
