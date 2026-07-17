package fr.renblood.npcshopkeeper.manager.road;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RoadTickScheduler {
    private static final int FULL_ROUTE_RECHECK_SECONDS = 300;
    private static final PriorityQueue<ScheduledRoadSpawn> QUEUE =
            new PriorityQueue<>(Comparator.comparingLong(ScheduledRoadSpawn::dueTick));
    private static final Map<String, ScheduledRoadSpawn> BY_ROAD_ID = new HashMap<>();

    private RoadTickScheduler() {}

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ServerLevel level = event.getServer().overworld();
        long now = event.getServer().getTickCount();
        while (!QUEUE.isEmpty() && QUEUE.peek().dueTick() <= now) {
            ScheduledRoadSpawn scheduled = QUEUE.poll();
            if (BY_ROAD_ID.get(scheduled.road().getId()) != scheduled) continue;
            NpcSpawnerManager.SpawnResult result = NpcSpawnerManager.trySpawnNpcForRoad(level, scheduled.road());
            schedule(scheduled.road(), now, result);
        }
    }

    public static void registerRoad(CommercialRoad road) {
        if (!BY_ROAD_ID.containsKey(road.getId())) schedule(road, 0L, NpcSpawnerManager.SpawnResult.SPAWNED);
    }

    public static void unregisterRoad(CommercialRoad road) { BY_ROAD_ID.remove(road.getId()); }
    public static void clear() {
        QUEUE.clear();
        BY_ROAD_ID.clear();
    }
    public static long getNextDueTick() { return QUEUE.isEmpty() ? -1L : QUEUE.peek().dueTick(); }
    public static int scheduledCount() { return BY_ROAD_ID.size(); }

    private static void schedule(CommercialRoad road, long now, NpcSpawnerManager.SpawnResult lastResult) {
        int seconds = lastResult == NpcSpawnerManager.SpawnResult.SPAWNED
                ? NpcSpawnerManager.getRandomTime(road.getMinTimer(), road.getMaxTimer())
                : FULL_ROUTE_RECHECK_SECONDS;
        ScheduledRoadSpawn scheduled = new ScheduledRoadSpawn(road, now + seconds * 20L);
        BY_ROAD_ID.put(road.getId(), scheduled);
        QUEUE.add(scheduled);
    }

    private record ScheduledRoadSpawn(CommercialRoad road, long dueTick) {}
}
