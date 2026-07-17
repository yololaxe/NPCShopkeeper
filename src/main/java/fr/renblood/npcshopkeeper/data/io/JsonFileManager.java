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

    public static void writeJsonFile(String writePath, JsonObject object) {
        if (writePath == null) {
            LOGGER.error("Tentative d'ecriture dans un fichier avec un chemin null.");
            return;
        }
        Path target = Path.of(writePath);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            try (FileWriter writer = new FileWriter(temp.toFile(), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(object, writer);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            JsonRepository.invalidate(target);
            WRITE_COUNT.incrementAndGet();
        } catch (IOException e) {
            LOGGER.error("Ecriture JSON echouee: {}", writePath, e);
        }
    }

    public static long getReadCount() { return READ_COUNT.get(); }
    public static long getWriteCount() { return WRITE_COUNT.get(); }
}
