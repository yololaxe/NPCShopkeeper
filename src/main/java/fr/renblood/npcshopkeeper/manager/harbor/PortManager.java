package fr.renblood.npcshopkeeper.manager.harbor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PortManager {
    private static final Logger LOGGER = LogManager.getLogger(PortManager.class);
    private static String PATH_PORTS;
    private static JsonRepository<Port> repository;
    
    // Liste locale pour le client (synchronisée via packet)
    private static List<Port> clientPorts = new ArrayList<>();
    
    // Configuration du prix (par défaut 50 blocs pour 1 fer)
    private static int blocksPerIron = 50;

    public static void init(MinecraftServer server) {
        PATH_PORTS = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/ports.json").toString();
        repository = new JsonRepository<>(
                Paths.get(PATH_PORTS),
                "ports",
                Port::fromJson,
                Port::toJson
        );
        loadConfig();
        LOGGER.info("PortManager initialisé : " + PATH_PORTS);
    }

    public static void addPort(Port port) {
        if (repository == null) return;
        List<Port> ports = repository.loadAll();
        ports.removeIf(p -> p.getName().equalsIgnoreCase(port.getName()));
        ports.add(port);
        repository.saveAll(ports);
        LOGGER.info("Port enregistré : " + port.getName());
    }

    public static List<Port> getAllPorts() {
        if (repository == null) {
            return clientPorts;
        }
        return repository.loadAll();
    }
    
    public static void setClientPorts(List<Port> ports) {
        clientPorts = ports;
        LOGGER.info("Ports synchronisés côté client : " + ports.size());
    }

    public static Optional<Port> getPort(String name) {
        return getAllPorts().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    // Gestion du prix
    public static int getBlocksPerIron() {
        return blocksPerIron;
    }

    public static void setBlocksPerIron(int value) {
        blocksPerIron = value;
        saveConfig();
    }

    // Sauvegarde/Chargement simple de la config dans le même fichier ports.json (ou un fichier dédié si on préfère)
    // Pour simplifier et ne pas casser JsonRepository qui gère une liste, on va utiliser un fichier séparé "harbor_config.json"
    // ou on triche en lisant/écrivant manuellement.
    // Allons au plus simple : un fichier dédié "harbor_config.json".
    
    private static void loadConfig() {
        try {
            String configPath = PATH_PORTS.replace("ports.json", "harbor_config.json");
            JsonObject json = JsonFileManager.readJsonFile(configPath);
            if (json.has("blocksPerIron")) {
                blocksPerIron = json.get("blocksPerIron").getAsInt();
            }
        } catch (Exception e) {
            LOGGER.error("Erreur chargement config port", e);
        }
    }

    private static void saveConfig() {
        try {
            String configPath = PATH_PORTS.replace("ports.json", "harbor_config.json");
            JsonObject json = new JsonObject();
            json.addProperty("blocksPerIron", blocksPerIron);
            JsonFileManager.writeJsonFile(configPath, json);
        } catch (Exception e) {
            LOGGER.error("Erreur sauvegarde config port", e);
        }
    }
}
