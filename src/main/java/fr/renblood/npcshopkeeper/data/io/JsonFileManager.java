package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicLong;

public class JsonFileManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicLong READ_COUNT = new AtomicLong();
    private static final AtomicLong WRITE_COUNT = new AtomicLong();

    public static String path = OnServerStartedManager.PATH;
    public static String pathHistory = OnServerStartedManager.PATH_HISTORY;
    public static String pathConstant = OnServerStartedManager.PATH_CONSTANT;
    public static String pathCommercial = OnServerStartedManager.PATH_COMMERCIAL;
    public static String pathNpcs = OnServerStartedManager.PATH_NPCS;

    public static JsonObject readJsonFile(String readPath) {
        if (readPath == null) {
            LOGGER.error("Tentative de lecture d'un fichier avec un chemin null.");
            return new JsonObject();
        }
        Path path = Path.of(readPath);
        try {
            if (!Files.exists(path)) return new JsonObject();
            READ_COUNT.incrementAndGet();
            JsonElement element = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            LOGGER.error("Lecture JSON echouee: {}", readPath, e);
            return new JsonObject();
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<Path, Object> WRITE_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();
    static Object writeLock(Path file) {
        return WRITE_LOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), ignored -> new Object());
    }

    /** Compatibility entry point: failure is observable by the caller, never a success acknowledgement. */
    public static void writeJsonFile(String writePath, JsonObject object) {
        try { writeJsonFileOrThrow(writePath, object); }
        catch (IOException failure) {
            LOGGER.error("Ecriture JSON echouee: {}", writePath, failure);
            throw new java.io.UncheckedIOException("Sauvegarde locale non confirmée", failure);
        }
    }

    public static void writeJsonFileOrThrow(String writePath, JsonObject object) throws IOException {
        if (writePath == null) throw new IOException("Chemin de sauvegarde absent");
        Path target = Path.of(writePath).toAbsolutePath().normalize();
        synchronized (writeLock(target)) {
            Files.createDirectories(target.getParent());
            Path temp = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
            try {
                try (var writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(object, writer);
                }
                try { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
                catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
                JsonRepository.invalidate(target);
                WRITE_COUNT.incrementAndGet();
            } finally { Files.deleteIfExists(temp); }
        }
    }

    /** Read/modify/write share the same path lock as repository saves. Existing fields are preserved. */
    public static void updateJsonFile(String filePath, java.util.function.Consumer<JsonObject> change) throws IOException {
        if (filePath == null) throw new IOException("Chemin de sauvegarde absent");
        Path target = Path.of(filePath).toAbsolutePath().normalize();
        synchronized (writeLock(target)) {
            JsonObject object = new JsonObject();
            if (Files.exists(target)) {
                READ_COUNT.incrementAndGet();
                try {
                    JsonElement parsed = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject()) throw new IOException("Le catalogue existant est invalide");
                    object = parsed.getAsJsonObject();
                } catch (com.google.gson.JsonParseException invalid) { throw new IOException("Le catalogue existant est illisible", invalid); }
            }
            change.accept(object);
            writeJsonFileOrThrow(target.toString(), object);
        }
    }

    public static long getReadCount() { return READ_COUNT.get(); }
    public static long getWriteCount() { return WRITE_COUNT.get(); }
}
