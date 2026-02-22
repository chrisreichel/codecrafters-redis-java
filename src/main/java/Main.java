import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
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
