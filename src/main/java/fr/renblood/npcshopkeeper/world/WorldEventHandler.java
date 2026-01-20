// src/main/java/fr/renblood/npcshopkeeper/world/WorldEventHandler.java
package fr.renblood.npcshopkeeper.world;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldEventHandler {
    private static final Logger LOGGER = LogManager.getLogger(WorldEventHandler.class);

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        File folder = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper").toFile();
        initializeFile(folder, "trade.json",         "{\"trades\": []}");
        initializeFile(folder, "trade_history.json", "{\"history\": []}");
        initializeFile(folder, "price_references.json", "{\"references\": []}");
        initializeFile(folder, "commercial_road.json",  "{\"roads\": []}");
        initializeFile(folder, "trades_npcs.json",     "{\"npcs\": []}");
        initializeFile(folder, "constant.json",         getDefaultConstantJson());
    }

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            NpcSpawnerManager.saveNpcData(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (!(evt.getLevel() instanceof ServerLevel world)) return;
        Entity entity = evt.getEntity();
        if (!(entity instanceof TradeNpcEntity npcEnt)) return;

        TradeNpc model = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_NPCS),
                "npcs",
                TradeNpc::fromJson,
                TradeNpc::toJson
        ).loadAll().stream()
                .filter(n -> n.getNpcId().equals(npcEnt.getUUID().toString()))
                .findFirst().orElse(null);

        if (model == null) {
            LOGGER.warn("onEntityJoin: aucun modèle TradeNpc trouvé pour UUID {}", npcEnt.getUUID());
        } else {
            LOGGER.info("onEntityJoin: modèle trouvé pour {} → name='{}', texture='{}', routeNpc={}",
                    npcEnt.getUUID(),
                    model.getNpcName(),
                    model.getTexture(),
                    model.isRouteNpc()
            );
            npcEnt.setTradeNpc(model);
            LOGGER.info("onEntityJoin: appliqué à l'entité {} → ent.getTradeNpc().getTexture()='{}'",
                    npcEnt.getUUID(),
                    npcEnt.getTradeNpc().getTexture()
            );
        }
    }

    private static void initializeFile(File folder, String name, String content) {
        File file = new File(folder, name);
        if (!file.exists()) {
            folder.mkdirs();
            try (FileWriter w = new FileWriter(file)) {
                w.write(content);
                LOGGER.info("Créé {} dans {}", name, folder);
            } catch (IOException e) {
                LOGGER.error("Erreur lors de la création de {}", name, e);
            }
        }
    }



private static String getDefaultConstantJson() {
        // Contenu JSON par defaut pour constant.json
        return """
        {
          "npcs": {
            "Bingo": {
              "Texture": "textures/entity/bingo.png",
              "IsShopkeeper": true,
              "Texts": [
                "Bienvenue dans le jardin ! Pret a commencer ?",
                "Tu devrais planter des melons, ils poussent vite !",
                "Cet endroit est parfait pour se detendre."
              ]
            },
            "George": {
              "Texture": "textures/entity/george.png",
              "IsShopkeeper": true,
              "Texts": [
                "J'aime ce que tu as fait ici.",
                "Les animaux adorent cet endroit.",
                "Tu pourrais ajouter une serre ?"
              ]
            },
            "Diana": {
              "Texture": "textures/entity/diana.png",
              "IsShopkeeper": true,
              "Texts": [
                "Tes recoltes sont impressionnantes.",
                "Les fruits de ton travail sont magnifiques.",
                "Tu prends vraiment soin de ton jardin."
              ]
            },
            "Bazaar": {
              "Texture": "textures/entity/bazaar.png",
              "IsShopkeeper": true,
              "Texts": [
                "Besoin d'aide pour le jardin ?",
                "Je peux te fournir ce qu'il te faut.",
                "Pense a ajouter des fleurs rares ici."
              ]
            },
            "Jerry": {
              "Texture": "textures/entity/jerry.png",
              "IsShopkeeper": true,
              "Texts": [
                "C'est agreable de voir le jardin grandir.",
                "Ton jardin attire beaucoup d'attention.",
                "N'oublie pas de nourrir tes animaux."
              ]
            },
            "Derpy": {
              "Texture": "textures/entity/derpy.png",
              "IsShopkeeper": true,
              "Texts": [
                "Pourquoi pas planter du ble ici ?",
                "Les betteraves poussent bien ici.",
                "Et si tu essayais de cultiver des champignons ?"
              ]
            },
            "Paul": {
              "Texture": "textures/entity/paul.png",
              "IsShopkeeper": true,
              "Texts": [
                "Tes recoltes de carottes sont incroyables !",
                "Tu devrais vendre tes surplus.",
                "Un bon fermier connait ses sols."
              ]
            },
            "Sam": {
              "Texture": "textures/entity/sam.png",
              "IsShopkeeper": true,
              "Texts": [
                "Ce jardin est parfait pour se relaxer.",
                "J'aime ton organisation.",
                "Tu devrais peut-etre installer un bassin."
              ]
            },
            "Tia": {
              "Texture": "textures/entity/tia.png",
              "IsShopkeeper": true,
              "Texts": [
                "As-tu besoin d'engrais ?",
                "Ce sol est parfait pour planter.",
                "Je peux t'aider avec tes plantes."
              ]
            },
            "Adventurer": {
              "Texture": "textures/entity/adventurer.png",
              "IsShopkeeper": true,
              "Texts": [
                "Ce jardin est un endroit calme.",
                "Il offre une pause loin de l'agitation.",
                "Ces fruits sont parfaits pour mes voyages."
              ]
            }
          }
        }
        """;
    }

}