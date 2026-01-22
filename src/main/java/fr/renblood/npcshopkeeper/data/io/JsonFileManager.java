package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.*;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileManager {
    private static final Logger LOGGER = LogManager.getLogger();

    // Ces variables statiques sont initialisées au chargement de la classe,
    // AVANT que OnServerStartedManager.PATH ne soit défini (qui est null au début).
    // Donc elles valent null et ne sont jamais mises à jour.
    
    // Solution : Utiliser des getters ou accéder directement à OnServerStartedManager
    
    public static String path         = OnServerStartedManager.PATH;
    public static String pathHistory  = OnServerStartedManager.PATH_HISTORY;
    public static String pathConstant = OnServerStartedManager.PATH_CONSTANT;
    public static String pathCommercial = OnServerStartedManager.PATH_COMMERCIAL;
    public static String pathNpcs     = OnServerStartedManager.PATH_NPCS;

    /** Lecture brute d’un fichier JSON */
    public static JsonObject readJsonFile(String readPath) {
        if (readPath == null) {
            LOGGER.error("Tentative de lecture d'un fichier avec un chemin null !");
            return new JsonObject();
        }
        
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
        if (writePath == null) {
            LOGGER.error("Tentative d'écriture dans un fichier avec un chemin null !");
            return;
        }

        try (FileWriter w = new FileWriter(Path.of(writePath).toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(obj, w);
        } catch (IOException e) {
            LOGGER.error("Écriture JSON échouée: {}", writePath, e);
        }
    }
}
