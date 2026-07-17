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
        List<?> cached = CACHE.get(cacheKey());
        if (cached != null) {
            CACHE_HITS.incrementAndGet();
            return new ArrayList<>((List<T>) cached);
        }
        CACHE_MISSES.incrementAndGet();
        JsonObject root = readJsonFile(file.toString());
        if (root == null || !root.has(rootKey) || !root.get(rootKey).isJsonArray()) {
            return List.of();
        }

        JsonArray array = root.getAsJsonArray(rootKey);
        List<T> list = new ArrayList<>(array.size());
        for (JsonElement elem : array) {
            if (!elem.isJsonObject()) {
                LOGGER.warn("Skipping non-object entry in '{}' array: {}", rootKey, elem);
                continue;
            }
            try {
                T item = deserializer.apply(elem.getAsJsonObject());
                list.add(item);
            } catch (Exception e) {
                LOGGER.error("Failed to parse element in '{}' array: {}", rootKey, elem, e);
            }
        }
        CACHE.put(cacheKey(), List.copyOf(list));
        return new ArrayList<>(list);
    }

    public void saveAll(Collection<T> items) {
        JsonArray arr = new JsonArray();
        for (T item : items) {
            JsonObject json = serializer.apply(item);
            arr.add(json);
        }
        JsonObject root = new JsonObject();
        root.add(rootKey, arr);
        JsonFileManager.writeJsonFile(file.toString(), root);
        CACHE.put(cacheKey(), List.copyOf(items));
    }

    public void add(T item) {
        List<T> list = new ArrayList<>(loadAll());
        list.add(item);
        saveAll(list);
    }

    private String cacheKey() {
        return file.toAbsolutePath().normalize() + "#" + rootKey;
    }

    public static void invalidate(Path file) {
        String prefix = file.toAbsolutePath().normalize() + "#";
        CACHE.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static void invalidateAll() { CACHE.clear(); }
    public static long getCacheHits() { return CACHE_HITS.get(); }
    public static long getCacheMisses() { return CACHE_MISSES.get(); }
    public static int getCachedRepositoryCount() { return CACHE.size(); }

    // À l'avenir, vous pourrez ajouter remove(Predicate<T> filter) si besoin
}
