package fr.renblood.npcshopkeeper.manager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

@Mod.EventBusSubscriber
public class ServerTickHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            MinecraftServer server = event.getServer();
            if (server != null) {
                ServerLevel level = server.overworld();
                RoadTickScheduler.tick(level);
            }
        }
    }
}
