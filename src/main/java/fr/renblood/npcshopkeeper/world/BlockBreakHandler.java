package fr.renblood.npcshopkeeper.world;

import fr.renblood.npcshopkeeper.procedures.route.PointDefiningModeProcedure;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.server.level.ServerPlayer;

@Mod.EventBusSubscriber
public class BlockBreakHandler {
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (PointDefiningModeProcedure.isInMode(player)) {
            PointDefiningModeProcedure mode = PointDefiningModeProcedure.getMode(player);
            mode.placePoint(event.getPos());
            event.setCanceled(true); // Empêche le bloc de réellement se casser
        }
    }
}
