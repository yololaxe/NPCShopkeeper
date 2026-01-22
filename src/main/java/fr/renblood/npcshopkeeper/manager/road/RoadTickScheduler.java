package fr.renblood.npcshopkeeper.manager.road;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RoadTickScheduler {
    private static final Map<CommercialRoad, Integer> timers = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(RoadTickScheduler.class);

    /** appelé chaque tick serveur (Phase.END) */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().overworld();
        tick(level);
    }

    public static void tick(ServerLevel level) {
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            int current = timers.getOrDefault(road, 0);
            
            // Npcshopkeeper.debugLog(LOGGER, "⏱️ Tick - Road : {} | timer={}", road.getName(), current); // Commenté

            if (current <= 0) {
                LOGGER.info("🔁 Timer écoulé pour : {}", road.getName());
                NpcSpawnerManager.trySpawnNpcForRoad(level, road);

                // Reset du timer en ticks (secondes*20)
                int delaySeconds = NpcSpawnerManager.getRandomTime(road.getMinTimer(), road.getMaxTimer());
                int delayTicks   = delaySeconds * 20;
                timers.put(road, delayTicks);
                LOGGER.info("⏳ Prochain spawn dans {} ticks pour : {}", delayTicks, road.getName());
            } else {
                timers.put(road, current - 1);
            }
        }
    }

    /** à appeler dès qu’on ajoute une nouvelle route pour lancer son premier spawn immediat */
    public static void registerRoad(CommercialRoad road) {
        if (!timers.containsKey(road)) {
            // Initialiser avec un délai aléatoire au lieu de 0 pour éviter le double spawn immédiat
            int delaySeconds = NpcSpawnerManager.getRandomTime(road.getMinTimer(), road.getMaxTimer());
            int delayTicks = delaySeconds * 20;
            timers.put(road, delayTicks);
            LOGGER.info("📥 Route enregistrée dans RoadTickScheduler : {} (Délai initial: {} ticks)", road.getName(), delayTicks);
        }
    }
}
