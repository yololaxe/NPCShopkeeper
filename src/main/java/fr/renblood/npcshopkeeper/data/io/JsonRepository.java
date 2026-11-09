package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static fr.renblood.npcshopkeeper.data.io.JsonFileManager.readJsonFile;

public class JsonRepository<T> {
    private static final Logger LOGGER = LogManager.getLogger(JsonRepository.class);
    private static final Map<String, List<?>> CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong CACHE_EPOCH = new AtomicLong();
    private static final Set<String> INCOMPLETE = ConcurrentHashMap.newKeySet();
    private static final AtomicLong CACHE_HITS = new AtomicLong();
    private static final AtomicLong CACHE_MISSES = new AtomicLong();

    private final Path file;
    private final String rootKey;
    private final Function<JsonObject, T> deserializer;
    private final Function<T, JsonObject> serializer;

    public JsonRepository(Path file,
                          String rootKey,
                          Function<JsonObject, T> deserializer,
                          Function<T, JsonObject> serializer) {
        this.file = file;
        this.rootKey = rootKey;
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    public List<T> loadAll() {
        return new ArrayList<>(snapshot());
    }

    /** Immutable collection view; mutable model objects keep their established save semantics. */
    @SuppressWarnings("unchecked")
    public List<T> snapshot() {
        String key = cacheKey();
        List<?> cached = CACHE.get(key);
        if (cached != null) { CACHE_HITS.incrementAndGet(); return (List<T>) cached; }
        synchronized (JsonFileManager.writeLock(file)) {
            cached = CACHE.get(key);
            if (cached != null) { CACHE_HITS.incrementAndGet(); return (List<T>) cached; }
            long epoch = CACHE_EPOCH.get();
            List<T> loaded = readSnapshot();
            if (loaded != null && CACHE_EPOCH.get() == epoch) CACHE.put(key, loaded);
            return loaded == null ? List.of() : loaded;
        }
    }

    private List<T> readSnapshot() {
        CACHE_MISSES.incrementAndGet();
        JsonObject root = readJsonFile(file.toString());
        if (root == null || !root.has(rootKey) || !root.get(rootKey).isJsonArray()) {
            INCOMPLETE.add(cacheKey());
            return null; // Failed reads are not cached as a valid empty collection.
        }

        JsonArray array = root.getAsJsonArray(rootKey);
        List<T> list = new ArrayList<>(array.size());
        boolean complete = true;
        for (JsonElement elem : array) {
            if (!elem.isJsonObject()) {
                complete = false;
                LOGGER.warn("Skipping non-object entry in '{}' array: {}", rootKey, elem);
                continue;
            }
            try {
                T item = deserializer.apply(elem.getAsJsonObject());
                if (item != null) list.add(item); else complete = false;
            } catch (Exception e) {
                complete = false;
                LOGGER.error("Failed to parse element in '{}' array: {}", rootKey, elem, e);
            }
        }
        if (complete) INCOMPLETE.remove(cacheKey()); else INCOMPLETE.add(cacheKey());
        return List.copyOf(list);
    }

    public void saveAll(Collection<T> items) {
        synchronized (JsonFileManager.writeLock(file)) {
            try {
                JsonArray arr = new JsonArray();
                for (T item : items) arr.add(serializer.apply(item));
                JsonObject root = new JsonObject(); root.add(rootKey, arr);
                JsonFileManager.writeJsonFileOrThrow(file.toString(), root);
                CACHE.put(cacheKey(), Collections.unmodifiableList(new ArrayList<>(items)));
                INCOMPLETE.remove(cacheKey());
            } catch (java.io.IOException failure) {
                invalidate(file); // Mutable model instances may already have been edited by the caller.
                throw new java.io.UncheckedIOException("Sauvegarde locale non confirmée : " + rootKey, failure);
            } catch (RuntimeException failure) {
                invalidate(file);
                throw failure;
            }
        }
    }

    public void add(T item) {
        synchronized (JsonFileManager.writeLock(file)) {
            List<T> list = loadAll();
            if (INCOMPLETE.contains(cacheKey()) && java.nio.file.Files.exists(file))
                throw new java.io.UncheckedIOException(new java.io.IOException("Le fichier existant doit être vérifié avant un nouvel ajout : " + rootKey));
            list.add(item); saveAll(list);
        }
    }

    private String cacheKey() {
        return file.toAbsolutePath().normalize() + "#" + rootKey;
    }

    public static void invalidate(Path file) {
        synchronized (JsonFileManager.writeLock(file)) {
            String prefix = file.toAbsolutePath().normalize() + "#";
            CACHE.keySet().removeIf(key -> key.startsWith(prefix));
            INCOMPLETE.removeIf(key -> key.startsWith(prefix));
        }
    }

    public static void invalidateAll() { CACHE_EPOCH.incrementAndGet(); CACHE.clear(); INCOMPLETE.clear(); }
    public static long getCacheHits() { return CACHE_HITS.get(); }
    public static long getCacheMisses() { return CACHE_MISSES.get(); }
    public static int getCachedRepositoryCount() { return CACHE.size(); }

    // À l'avenir, vous pourrez ajouter remove(Predicate<T> filter) si besoin
}
