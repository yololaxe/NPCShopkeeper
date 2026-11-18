package fr.renblood.npcshopkeeper.world.inventory;
import fr.renblood.npcshopkeeper.procedures.route.PointDefiningModeProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="npcshopkeeper")
public class ParticleUpdateHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || PointDefiningModeProcedure.activeModes.isEmpty())return;
        if(event.getServer().getTickCount()%5!=0)return;
        PointDefiningModeProcedure.activeModes.entrySet().removeIf(entry->!entry.getValue().isValid());
        PointDefiningModeProcedure.activeModes.values().forEach(PointDefiningModeProcedure::updateParticles);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player)PointDefiningModeProcedure.stop(player);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event){
        PointDefiningModeProcedure.activeModes.clear();
    }
}
