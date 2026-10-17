package org.flipkart.repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

abstract class AbstractInMemoryRepository<T> {
    private final ConcurrentHashMap<String, T> entriesById = new ConcurrentHashMap<>();

    protected final void save(String id, T value) {
        entriesById.put(id, value);
    }

    protected final T lookup(String id) {
        return entriesById.get(id);
    }

    protected final List<T> snapshot() {
        return List.copyOf(entriesById.values());
    }

    protected final void delete(String id) {
        entriesById.remove(id);
    }
}
