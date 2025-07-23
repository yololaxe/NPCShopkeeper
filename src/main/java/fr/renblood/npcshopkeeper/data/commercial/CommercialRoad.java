package fr.renblood.npcshopkeeper.data.commercial;

import com.google.gson.*;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.mojang.text2speech.Narrator.LOGGER;

public class CommercialRoad {
    private String id;
    private String name;
    private String category;
    private ArrayList<BlockPos> positions;
    private ArrayList<TradeNpcEntity> npcEntities;
    private int minTimer;
    private int maxTimer;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ArrayList<BlockPos> getPositions() {
        return positions;
    }


    public void setPositions(ArrayList<BlockPos> positions) {
        this.positions = positions;
    }

    public ArrayList<TradeNpcEntity> getNpcEntities() {
        return npcEntities;
    }

    public void addNpcAndPersist(TradeNpcEntity npc, List<CommercialRoad> allRoads) {
        this.npcEntities.add(npc);

        // → Récupère le ServerLevel via la méthode, pas le champ privé
        ServerLevel world = (ServerLevel) npc.level();   // ou npc.getLevel()

        JsonRepository<CommercialRoad> repo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, world),
                CommercialRoad::toJson
        );
        List<CommercialRoad> fromDisk = repo.loadAll();

        for (CommercialRoad r : fromDisk) {
            if (r.getId().equals(this.getId())) {
                Set<String> uuids = r.getNpcEntities().stream()
                        .map(e -> e.getUUID().toString())
                        .collect(Collectors.toSet());
                if (!uuids.contains(npc.getUUID().toString())) {
                    r.getNpcEntities().add(npc);
                }
                break;
            }
        }

        repo.saveAll(fromDisk);
    }

    public void removeNpcAndPersist(TradeNpcEntity npc) {
        // 1) Retire de la liste en mémoire
        this.npcEntities.removeIf(e -> e.getUUID().equals(npc.getUUID()));

        // 2) Met à jour le JSON
        Path path = Paths.get(OnServerStartedManager.PATH_COMMERCIAL);
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray roadsArr = root.getAsJsonArray("roads");

            for (JsonElement el : roadsArr) {
                JsonObject rj = el.getAsJsonObject();
                if (!rj.get("id").getAsString().equals(this.id)) continue;

                JsonArray npcArr = rj.getAsJsonArray("npcEntities");
                // on supprime tous les éléments dont l'uuid correspond
                for (int i = npcArr.size() - 1; i >= 0; i--) {
                    JsonElement je = npcArr.get(i);
                    if (!je.isJsonObject()) continue;
                    JsonObject jo = je.getAsJsonObject();
                    if (jo.has("uuid") && jo.get("uuid").getAsString().equals(npc.getUUID().toString())) {
                        npcArr.remove(i);
                    }
                }
                break;
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                new Gson().toJson(root, writer);
            }
            LOGGER.info("💾 PNJ {} supprimé de la route '{}' et JSON mis à jour", npc.getUUID(), this.name);
        } catch (IOException ex) {
            LOGGER.error("❌ Impossible de persister la suppression du PNJ {} sur la route {}", npc.getUUID(), this.name, ex);
        }
    }


    public void setNpcEntities(ArrayList<TradeNpcEntity> npcEntities) {
        this.npcEntities = npcEntities;
    }

    public int getMinTimer() {
        return minTimer;
    }

    public void setMinTimer(int minTimer) {
        this.minTimer = minTimer;
    }

    public int getMaxTimer() {
        return maxTimer;
    }

    public void setMaxTimer(int maxTimer) {
        this.maxTimer = maxTimer;
    }
    public void removeNpcEntity(TradeNpcEntity npc) {
        this.npcEntities.remove(npc);
    }

    public CommercialRoad(String id, String name, String category, ArrayList<BlockPos> positions, ArrayList<TradeNpcEntity> npcEntities, int minTimer, int maxTimer) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.positions = positions;
        this.npcEntities = npcEntities;
        this.minTimer = minTimer;
        this.maxTimer = maxTimer;
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }


    public static void updateCommercialRoadAfterRemoval(TradeNpcEntity npc) {
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            if (road.getNpcEntities().contains(npc)) {
                road.removeNpcEntity(npc);
                LOGGER.info("NPC supprimé de la route commerciale : " + road.getName());
                return;
            }
        }
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("name", name);
        o.addProperty("category", category);
        o.addProperty("minTimer", minTimer);
        o.addProperty("maxTimer", maxTimer);

        JsonArray posArr = new JsonArray();
        for (BlockPos p : positions) {
            JsonObject pj = new JsonObject();
            pj.addProperty("x", p.getX());
            pj.addProperty("y", p.getY());
            pj.addProperty("z", p.getZ());
            posArr.add(pj);
        }
        o.add("positions", posArr);

        JsonArray npcArr = new JsonArray();
        for (TradeNpcEntity npc : npcEntities) {
            JsonObject nj = new JsonObject();
            nj.addProperty("uuid", npc.getUUID().toString());
            npcArr.add(nj);
        }
        o.add("npcEntities", npcArr);

        return o;
    }

    /**
     * Désérialise une route depuis le JsonObject.
     * @param o      l’objet JSON
     * @param world  le ServerLevel pour retrouver les entités
     */
    public static CommercialRoad fromJson(JsonObject o, ServerLevel world) {
        String id       = o.get("id").getAsString();
        String name     = o.get("name").getAsString();
        String category = o.get("category").getAsString();
        int minTimer    = o.get("minTimer").getAsInt();
        int maxTimer    = o.get("maxTimer").getAsInt();

        // positions
        List<BlockPos> positions = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("positions")) {
            JsonObject pj = e.getAsJsonObject();
            positions.add(
                    new BlockPos(
                            pj.get("x").getAsInt(),
                            pj.get("y").getAsInt(),
                            pj.get("z").getAsInt()
                    )
            );
        }

        // npcEntities
        List<TradeNpcEntity> npcEntities = new ArrayList<>();
        if (o.has("npcEntities")) {
            for (JsonElement e : o.getAsJsonArray("npcEntities")) {
                String uuid = e.getAsJsonObject().get("uuid").getAsString();
                Entity ent = world.getEntity(UUID.fromString(uuid));
                if (ent instanceof TradeNpcEntity tne) {
                    npcEntities.add(tne);
                }
            }
        }

        return new CommercialRoad(id, name, category,
                new ArrayList<>(positions),
                new ArrayList<>(npcEntities),
                minTimer, maxTimer);
    }

}