import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    record Entry(String value, long expiresAt) {}
    private static final Map<String, Entry> store = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        int port = 6379;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.startVirtualThread(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            var out = clientSocket.getOutputStream();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("*")) {
                    int numElements = Integer.parseInt(line.substring(1));
                    String[] elements = new String[numElements];
                    for (int i = 0; i < numElements; i++) {
                        reader.readLine(); // skip $<length>
                        elements[i] = reader.readLine();
                    }

                    String command = elements[0].toUpperCase();
                    if (command.equals("PING")) {
                        out.write("+PONG\r\n".getBytes());
                    } else if (command.equals("ECHO")) {
                        String arg = elements[1];
                        out.write(("$" + arg.length() + "\r\n" + arg + "\r\n").getBytes());
                    } else if (command.equals("SET")) {
                        String key = elements[1];
                        String value = elements[2];
                        long expiresAt = -1;
                        for (int i = 3; i < elements.length - 1; i++) {
                            if (elements[i].equalsIgnoreCase("PX")) {
                                expiresAt = System.currentTimeMillis() + Long.parseLong(elements[i + 1]);
                            } else if (elements[i].equalsIgnoreCase("EX")) {
                                expiresAt = System.currentTimeMillis() + Long.parseLong(elements[i + 1]) * 1000;
                            }
                        }
                        store.put(key, new Entry(value, expiresAt));
                        out.write("+OK\r\n".getBytes());
                    } else if (command.equals("GET")) {
                        String key = elements[1];
                        Entry entry = store.get(key);
                        if (entry == null || (entry.expiresAt() != -1 && System.currentTimeMillis() > entry.expiresAt())) {
                            out.write("$-1\r\n".getBytes());
                        } else {
                            String value = entry.value();
                            out.write(("$" + value.length() + "\r\n" + value + "\r\n").getBytes());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        }
    }
}
