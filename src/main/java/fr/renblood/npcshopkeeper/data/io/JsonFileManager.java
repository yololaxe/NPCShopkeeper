package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.*;

public class JsonFileManager {
    private static final Logger LOGGER = LogManager.getLogger();

//    public static String path         = PATH;
//    public static String pathHistory  = PATH_HISTORY;
//    public static String pathConstant = PATH_CONSTANT;
//    public static String pathCommercial = PATH_COMMERCIAL;
//    public static String pathNpcs     = PATH_NPCS;

    /** Lecture brute d’un fichier JSON */
    public static JsonObject readJsonFile(String readPath) {
        Path p = Path.of(readPath);
        try {
            if (!Files.exists(p)) return new JsonObject();
            String content = Files.readString(p);
            JsonElement el = JsonParser.parseString(content);
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (IOException e) {
            LOGGER.error("Lecture JSON échouée: {}", readPath, e);
            return new JsonObject();
        }
    }

    /** Écriture brute d’un JsonObject dans un fichier */
    public static void writeJsonFile(String writePath, JsonObject obj) {
        try (FileWriter w = new FileWriter(Path.of(writePath).toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        } catch (IOException e) {
            LOGGER.error("Écriture JSON échouée: {}", writePath, e);
        }
    }
}
