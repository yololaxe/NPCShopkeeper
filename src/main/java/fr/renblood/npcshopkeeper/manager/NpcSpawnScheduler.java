package fr.renblood.npcshopkeeper.manager;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

@Mod.EventBusSubscriber
public class NpcSpawnScheduler {

    private static final Map<CommercialRoad, ScheduledSpawn> scheduledSpawns = new HashMap<>();

    public static void scheduleSpawn(ServerLevel level, CommercialRoad road, int delayTicks) {
        scheduledSpawns.put(road, new ScheduledSpawn(level, road, delayTicks));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<CommercialRoad, ScheduledSpawn>> it = scheduledSpawns.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<CommercialRoad, ScheduledSpawn> entry = it.next();
            ScheduledSpawn task = entry.getValue();
            task.ticksLeft--;

            if (task.ticksLeft <= 0) {
                NpcSpawnerManager.trySpawnNpcForRoad(task.level, task.road); // appel ton spawn
                int nextDelay = NpcSpawnerManager.getRandomTime(task.road.getMinTimer(), task.road.getMaxTimer()) * 20;
                scheduleSpawn(task.level, task.road, nextDelay);
                it.remove(); // Supprime l’ancienne tâche
            }
        }
    }

    private static class ScheduledSpawn {
        ServerLevel level;
        CommercialRoad road;
        int ticksLeft;

        public ScheduledSpawn(ServerLevel level, CommercialRoad road, int ticks) {
            this.level = level;
            this.road = road;
            this.ticksLeft = ticks;
        }
    }
}
