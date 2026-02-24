package br.net.reichel.redis.store.impl;

import br.net.reichel.redis.model.StreamEntry;
import br.net.reichel.redis.store.DataStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory implementation of {@link DataStore} backed by {@link ConcurrentHashMap}.
 */
public class InMemoryDataStore implements DataStore {

    private record Entry(String value, long expiresAt) {
        boolean isExpired() {
            return expiresAt != -1 && System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Entry> stringStore = new ConcurrentHashMap<>();
    private final Map<String, List<String>> listStore = new ConcurrentHashMap<>();
    private final Map<String, List<StreamEntry>> streamStore = new ConcurrentHashMap<>();
    private final Map<String, Object> listLocks = new ConcurrentHashMap<>();
    private final Map<String, Queue<CompletableFuture<String[]>>> blpopWaiters = new ConcurrentHashMap<>();
    private final Map<String, Queue<CompletableFuture<StreamEntry>>> xreadWaiters = new ConcurrentHashMap<>();

    private Object getLock(String key) {
        return listLocks.computeIfAbsent(key, k -> new Object());
    }

    @Override
    public void setString(String key, String value, long expiresAt) {
        stringStore.put(key, new Entry(value, expiresAt));
    }

    @Override
    public Optional<String> getString(String key) {
        Entry entry = stringStore.get(key);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Override
    public long getExpiry(String key) {
        Entry entry = stringStore.get(key);
        return entry == null ? -1 : entry.expiresAt();
    }

    @Override
    public String type(String key) {
        if (stringStore.containsKey(key)) return "string";
        if (listStore.containsKey(key)) return "list";
        if (streamStore.containsKey(key)) return "stream";
        return "none";
    }

    @Override
    public int rpush(String key, List<String> values) {
        int size;
        synchronized (getLock(key)) {
            List<String> list = listStore.computeIfAbsent(key, k -> new ArrayList<>());
            list.addAll(values);
            size = list.size();
            notifyBlpopWaiter(key, list);
        }
        return size;
    }

    @Override
    public int lpush(String key, List<String> values) {
        int size;
        synchronized (getLock(key)) {
            List<String> list = listStore.computeIfAbsent(key, k -> new ArrayList<>());
            for (String v : values) {
                list.add(0, v);
            }
            size = list.size();
            notifyBlpopWaiter(key, list);
        }
        return size;
    }

    @Override
    public int llen(String key) {
        List<String> list = listStore.get(key);
        return list == null ? 0 : list.size();
    }

    @Override
    public List<String> lrange(String key, int start, int stop) {
        List<String> list = listStore.get(key);
        if (list == null) return List.of();
        int size = list.size();
        if (start < 0) start = Math.max(0, size + start);
        if (stop < 0) stop = size + stop;
        if (start >= size || start > stop) return List.of();
        int end = Math.min(stop, size - 1);
        return new ArrayList<>(list.subList(start, end + 1));
    }

    @Override
    public Optional<String> lpop(String key) {
        List<String> list = listStore.get(key);
        if (list == null || list.isEmpty()) return Optional.empty();
        return Optional.of(list.remove(0));
    }

    @Override
    public List<String> lpopN(String key, int count) {
        List<String> list = listStore.get(key);
        if (list == null || list.isEmpty()) return List.of();
        int n = Math.min(count, list.size());
        List<String> result = new ArrayList<>(list.subList(0, n));
        list.subList(0, n).clear();
        return result;
    }

    @Override
    public Optional<String[]> blpopImmediate(String key, CompletableFuture<String[]> waiter) {
        synchronized (getLock(key)) {
            List<String> list = listStore.get(key);
            if (list != null && !list.isEmpty()) {
                return Optional.of(new String[]{key, list.remove(0)});
            }
            blpopWaiters.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).offer(waiter);
            return Optional.empty();
        }
    }

    @Override
    public void removeBlpopWaiter(String key, CompletableFuture<String[]> waiter) {
        Queue<CompletableFuture<String[]>> q = blpopWaiters.get(key);
        if (q != null) q.remove(waiter);
    }

    @Override
    public void appendStreamEntry(String key, String resolvedId, List<String> fields) {
        StreamEntry entry = new StreamEntry(resolvedId, new ArrayList<>(fields));
        streamStore.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        Queue<CompletableFuture<StreamEntry>> waiters = xreadWaiters.get(key);
        if (waiters != null) {
            CompletableFuture<StreamEntry> waiter = waiters.poll();
            if (waiter != null) waiter.complete(entry);
        }
    }

    @Override
    public Optional<StreamEntry> lastStreamEntry(String key) {
        List<StreamEntry> stream = streamStore.get(key);
        if (stream == null || stream.isEmpty()) return Optional.empty();
        return Optional.of(stream.get(stream.size() - 1));
    }

    @Override
    public List<StreamEntry> xrange(String key, long startMs, long startSeq, long endMs, long endSeq) {
        List<StreamEntry> stream = streamStore.get(key);
        if (stream == null) return List.of();
        List<StreamEntry> results = new ArrayList<>();
        for (StreamEntry entry : stream) {
            long ms = entry.millis(), seq = entry.sequence();
            if ((ms > startMs || (ms == startMs && seq >= startSeq)) &&
                (ms < endMs || (ms == endMs && seq <= endSeq))) {
                results.add(entry);
            }
        }
        return results;
    }

    @Override
    public List<StreamEntry> xreadAfter(String key, long afterMs, long afterSeq) {
        List<StreamEntry> stream = streamStore.get(key);
        if (stream == null) return List.of();
        List<StreamEntry> results = new ArrayList<>();
        for (StreamEntry entry : stream) {
            long ms = entry.millis(), seq = entry.sequence();
            if (ms > afterMs || (ms == afterMs && seq > afterSeq)) {
                results.add(entry);
            }
        }
        return results;
    }

    @Override
    public void registerXreadWaiter(String key, CompletableFuture<StreamEntry> waiter) {
        xreadWaiters.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).offer(waiter);
    }

    @Override
    public void removeXreadWaiter(String key, CompletableFuture<StreamEntry> waiter) {
        Queue<CompletableFuture<StreamEntry>> q = xreadWaiters.get(key);
        if (q != null) q.remove(waiter);
    }

    private void notifyBlpopWaiter(String key, List<String> list) {
        Queue<CompletableFuture<String[]>> waiters = blpopWaiters.get(key);
        if (waiters != null && !list.isEmpty()) {
            CompletableFuture<String[]> waiter = waiters.poll();
            if (waiter != null) {
                String val = list.remove(0);
                waiter.complete(new String[]{key, val});
            }
        }
    }
}
