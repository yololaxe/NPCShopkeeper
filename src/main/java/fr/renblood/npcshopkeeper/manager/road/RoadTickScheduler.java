package fr.renblood.npcshopkeeper.manager.road;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RoadTickScheduler {
    private static final Map<CommercialRoad, Integer> timers = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(RoadTickScheduler.class);
    public static void tick(ServerLevel level) {

        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            int current = timers.getOrDefault(road, -42);
            LOGGER.info("⏱️ Tick - Road : " + road.getName() + " | timer=" + current);

            if (current <= 0) {
                LOGGER.info("🔁 Timer écoulé pour : " + road.getName());

                NpcSpawnerManager.trySpawnNpcForRoad(level, road);

                int delay = NpcSpawnerManager.getRandomTime(road.getMinTimer(), road.getMaxTimer()) * 20;
                timers.put(road, delay);

                LOGGER.info("⏳ Prochain spawn dans " + delay + " ticks pour : " + road.getName());
            } else {
                timers.put(road, current - 1);
            }
        }
    }



    public static void registerRoad(CommercialRoad road) {
        if (!timers.containsKey(road)) {
            timers.put(road, 0); // Force un tick immédiat
            LOGGER.info("📥 Route enregistrée dans RoadTickScheduler : " + road.getName());
        }
    }


}
