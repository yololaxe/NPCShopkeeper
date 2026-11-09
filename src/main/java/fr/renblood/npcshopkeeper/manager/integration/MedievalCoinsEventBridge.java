package fr.renblood.npcshopkeeper.manager.integration;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcSpawnManager;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MedievalCoinsEventBridge {
    private static final Logger LOGGER = LogManager.getLogger(MedievalCoinsEventBridge.class);
    private static final Set<String> SPAWN_EVENTS = Set.of(
            "NpcSpawnCreatedEvent", "NpcSpawnMovedEvent", "NpcSpawnRemovedEvent");
    private static long eventCount;
    private static long lastEventAt;
    private static final ScheduledExecutorService SYNC_SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "NpcShopkeeper-MedievalCoins-Sync");
        thread.setDaemon(true);
        return thread;
    });
    private static final Set<String> PENDING_EVENTS = ConcurrentHashMap.newKeySet();
    private static final AtomicReference<net.minecraft.server.MinecraftServer> PENDING_SERVER = new AtomicReference<>();
    private static volatile boolean syncScheduled;

    private MedievalCoinsEventBridge() {}

    @SubscribeEvent
    public static void onEvent(Event event) {
        String className = event.getClass().getName();
        if (!className.startsWith("fr.renblood.medievalcoins.")) return;
        String simpleName = event.getClass().getSimpleName();
        if (!SPAWN_EVENTS.contains(simpleName)
                && !"NpcTemplateCreatedEvent".equals(simpleName)
                && !"NpcTemplateUpdatedEvent".equals(simpleName)
                && !"QuestUpdatedEvent".equals(simpleName)) return;

        eventCount++;
        lastEventAt = System.currentTimeMillis();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        String spawnId = readSpawnId(event);
        PENDING_SERVER.set(server);
        if (SPAWN_EVENTS.contains(simpleName) && spawnId != null && !spawnId.isBlank()) {
            PENDING_EVENTS.add(spawnId);
        } else if (!"QuestUpdatedEvent".equals(simpleName)) {
            PENDING_EVENTS.add("*");
        }
        scheduleSynchronization();
    }

    private static void scheduleSynchronization() {
        if (syncScheduled) return;
        syncScheduled = true;
        SYNC_SCHEDULER.schedule(() -> {
            syncScheduled = false;
            var server = PENDING_SERVER.getAndSet(null);
            boolean hasChanges = !PENDING_EVENTS.isEmpty();
            PENDING_EVENTS.clear();
            if (server != null && hasChanges) {
                GlobalNpcSpawnManager.synchronizeAsync(server);
            }
        }, 300, TimeUnit.MILLISECONDS);
    }

    private static String readSpawnId(Object event) {
        for (String methodName : new String[]{"getSpawnId", "spawnId"}) {
            try {
                Method method = event.getClass().getMethod(methodName);
                Object value = method.invoke(event);
                if (value != null) return value.toString();
            } catch (ReflectiveOperationException ignored) {
            }
        }
        for (String fieldName : new String[]{"spawnId", "spawn_id"}) {
            try {
                Field field = event.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(event);
                if (value != null) return value.toString();
            } catch (ReflectiveOperationException ignored) {
            }
        }
        LOGGER.warn("Evenement Medieval Coins sans spawnId lisible: {}", event.getClass().getName());
        return null;
    }

    public static long getEventCount() { return eventCount; }
    public static long getLastEventAt() { return lastEventAt; }
}
