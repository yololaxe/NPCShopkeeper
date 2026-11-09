package fr.renblood.npcshopkeeper.manager.npc;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NpcAutoRefreshManager {
    private static final int DIFFERENTIAL_INTERVAL_TICKS = 20 * 5;
    private static final int FALLBACK_INTERVAL_TICKS = 20 * 300;

    private NpcAutoRefreshManager() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % DIFFERENTIAL_INTERVAL_TICKS != 0) return;
        boolean differential = fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration.supportsDifferentialSync();
        int interval = differential ? DIFFERENTIAL_INTERVAL_TICKS : FALLBACK_INTERVAL_TICKS;
        if (event.getServer().getTickCount() % interval != 0) return;
        if (differential) GlobalNpcSpawnManager.synchronizeDifferentialAsync(event.getServer());
        else GlobalNpcSpawnManager.synchronizeAsync(event.getServer());
    }
}
