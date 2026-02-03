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
    
    // Configuration
    private static int blocksPerIron = 50;
    private static int dayLengthInMinutes = 20; // Défaut vanilla

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
    
    // Gestion de la durée du jour
    public static int getDayLengthInMinutes() {
        return dayLengthInMinutes;
    }

    public static void setDayLengthInMinutes(int value) {
        dayLengthInMinutes = value;
        saveConfig();
    }
    
    // Méthode pour mettre à jour la config côté client (sans sauvegarder)
    public static void setClientConfig(int blocks, int dayLength) {
        blocksPerIron = blocks;
        dayLengthInMinutes = dayLength;
        LOGGER.info("Config harbor synchronisée côté client : 1 fer/" + blocks + " blocs, Jour=" + dayLength + "min");
    }

    private static void loadConfig() {
        try {
            String configPath = PATH_PORTS.replace("ports.json", "harbor_config.json");
            JsonObject json = JsonFileManager.readJsonFile(configPath);
            if (json.has("blocksPerIron")) {
                blocksPerIron = json.get("blocksPerIron").getAsInt();
            }
            if (json.has("dayLengthInMinutes")) {
                dayLengthInMinutes = json.get("dayLengthInMinutes").getAsInt();
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
            json.addProperty("dayLengthInMinutes", dayLengthInMinutes);
            JsonFileManager.writeJsonFile(configPath, json);
        } catch (Exception e) {
            LOGGER.error("Erreur sauvegarde config port", e);
        }
    }
}
