import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Main {
    record Entry(String value, long expiresAt) {}
    private static final Map<String, Entry> store = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> listStore = new ConcurrentHashMap<>();
    private static final Map<String, List<String[]>> streamStore = new ConcurrentHashMap<>();
    private static final Map<String, Object> listLocks = new ConcurrentHashMap<>();
    private static final Map<String, Queue<CompletableFuture<String[]>>> blockedClients = new ConcurrentHashMap<>();

    private static Object getLock(String key) {
        return listLocks.computeIfAbsent(key, k -> new Object());
    }

    // Must be called inside synchronized(getLock(key))
    private static void notifyBlockedClient(String key, List<String> list) {
        Queue<CompletableFuture<String[]>> waiters = blockedClients.get(key);
        if (waiters != null && !list.isEmpty()) {
            CompletableFuture<String[]> waiter = waiters.poll();
            if (waiter != null) {
                String val = list.remove(0);
                waiter.complete(new String[]{key, val});
            }
        }
    }

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
                    } else if (command.equals("RPUSH")) {
                        String key = elements[1];
                        int size;
                        synchronized (getLock(key)) {
                            List<String> list = listStore.computeIfAbsent(key, k -> new ArrayList<>());
                            for (int i = 2; i < elements.length; i++) {
                                list.add(elements[i]);
                            }
                            size = list.size();
                            notifyBlockedClient(key, list);
                        }
                        out.write((":" + size + "\r\n").getBytes());
                    } else if (command.equals("LPUSH")) {
                        String key = elements[1];
                        int size;
                        synchronized (getLock(key)) {
                            List<String> list = listStore.computeIfAbsent(key, k -> new ArrayList<>());
                            for (int i = 2; i < elements.length; i++) {
                                list.add(0, elements[i]);
                            }
                            size = list.size();
                            notifyBlockedClient(key, list);
                        }
                        out.write((":" + size + "\r\n").getBytes());
                    } else if (command.equals("BLPOP")) {
                        String key = elements[1];
                        double timeout = Double.parseDouble(elements[elements.length - 1]);
                        CompletableFuture<String[]> future = null;
                        String[] result = null;
                        synchronized (getLock(key)) {
                            List<String> list = listStore.get(key);
                            if (list != null && !list.isEmpty()) {
                                result = new String[]{key, list.remove(0)};
                            } else {
                                future = new CompletableFuture<>();
                                blockedClients.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).offer(future);
                            }
                        }
                        if (future != null) {
                            try {
                                if (timeout == 0) {
                                    result = future.get();
                                } else {
                                    long timeoutMs = (long)(timeout * 1000);
                                    result = future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                                }
                            } catch (java.util.concurrent.TimeoutException e) {
                                future.cancel(true);
                                Queue<CompletableFuture<String[]>> waiters = blockedClients.get(key);
                                if (waiters != null) waiters.remove(future);
                                out.write("*-1\r\n".getBytes());
                                continue;
                            } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        String listKey = result[0];
                        String val = result[1];
                        out.write(("*2\r\n$" + listKey.length() + "\r\n" + listKey + "\r\n$" + val.length() + "\r\n" + val + "\r\n").getBytes());
                    } else if (command.equals("LPOP")) {
                        String key = elements[1];
                        List<String> list = listStore.get(key);
                        if (elements.length == 2) {
                            // No count argument — return single bulk string
                            if (list == null || list.isEmpty()) {
                                out.write("$-1\r\n".getBytes());
                            } else {
                                String val = list.remove(0);
                                out.write(("$" + val.length() + "\r\n" + val + "\r\n").getBytes());
                            }
                        } else {
                            // Count argument provided — return array
                            int count = Integer.parseInt(elements[2]);
                            if (list == null || list.isEmpty()) {
                                out.write("*0\r\n".getBytes());
                            } else {
                                int n = Math.min(count, list.size());
                                StringBuilder sb = new StringBuilder("*" + n + "\r\n");
                                for (int i = 0; i < n; i++) {
                                    String val = list.remove(0);
                                    sb.append("$").append(val.length()).append("\r\n").append(val).append("\r\n");
                                }
                                out.write(sb.toString().getBytes());
                            }
                        }
                    } else if (command.equals("LLEN")) {
                        String key = elements[1];
                        List<String> list = listStore.get(key);
                        int len = (list == null) ? 0 : list.size();
                        out.write((":" + len + "\r\n").getBytes());
                    } else if (command.equals("LRANGE")) {
                        String key = elements[1];
                        List<String> list = listStore.get(key);
                        if (list == null) {
                            out.write("*0\r\n".getBytes());
                        } else {
                            int size = list.size();
                            int start = Integer.parseInt(elements[2]);
                            int stop = Integer.parseInt(elements[3]);
                            if (start < 0) start = Math.max(0, size + start);
                            if (stop < 0) stop = size + stop;
                            if (start >= size || start > stop) {
                                out.write("*0\r\n".getBytes());
                            } else {
                                int end = Math.min(stop, size - 1);
                                List<String> sub = list.subList(start, end + 1);
                                StringBuilder sb = new StringBuilder("*" + sub.size() + "\r\n");
                                for (String elem : sub) {
                                    sb.append("$").append(elem.length()).append("\r\n").append(elem).append("\r\n");
                                }
                                out.write(sb.toString().getBytes());
                            }
                        }
                    } else if (command.equals("XADD")) {
                        String key = elements[1];
                        String id = elements[2];
                        String[] idParts = id.split("-");
                        long ms = Long.parseLong(idParts[0]);
                        long seq = Long.parseLong(idParts[1]);
                        if (ms == 0 && seq == 0) {
                            out.write("-ERR The ID specified in XADD must be greater than 0-0\r\n".getBytes());
                            continue;
                        }
                        List<String[]> stream = streamStore.get(key);
                        if (stream != null && !stream.isEmpty()) {
                            String[] lastEntry = stream.get(stream.size() - 1);
                            String[] lastParts = lastEntry[0].split("-");
                            long lastMs = Long.parseLong(lastParts[0]);
                            long lastSeq = Long.parseLong(lastParts[1]);
                            if (ms < lastMs || (ms == lastMs && seq <= lastSeq)) {
                                out.write("-ERR The ID specified in XADD is equal or smaller than the target stream top item\r\n".getBytes());
                                continue;
                            }
                        }
                        String[] entry = new String[elements.length - 2];
                        entry[0] = id;
                        for (int i = 3; i < elements.length; i++) {
                            entry[i - 2] = elements[i];
                        }
                        streamStore.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
                        out.write(("$" + id.length() + "\r\n" + id + "\r\n").getBytes());
                    } else if (command.equals("TYPE")) {
                        String key = elements[1];
                        String type;
                        if (store.containsKey(key)) {
                            type = "string";
                        } else if (listStore.containsKey(key)) {
                            type = "list";
                        } else if (streamStore.containsKey(key)) {
                            type = "stream";
                        } else {
                            type = "none";
                        }
                        out.write(("+" + type + "\r\n").getBytes());
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
