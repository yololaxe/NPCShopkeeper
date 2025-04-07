package fr.renblood.npcshopkeeper.manager;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;

public class RoadTickScheduler {
    private static final Map<CommercialRoad, Integer> timers = new HashMap<>();

    public static void tick(ServerLevel level) {
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            int current = timers.getOrDefault(road, 0);
            if (current <= 0) {
                NpcSpawnerManager.trySpawnNpcForRoad(level, road);
                int delay = NpcSpawnerManager.getRandomTime(road.getMinTimer(), road.getMaxTimer()) * 20;
                timers.put(road, delay);
            } else {
                timers.put(road, current - 1);
            }
        }
    }
}
