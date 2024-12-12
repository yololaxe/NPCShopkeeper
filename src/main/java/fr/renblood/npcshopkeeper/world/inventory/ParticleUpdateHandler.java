package fr.renblood.npcshopkeeper.world.inventory;
import fr.renblood.npcshopkeeper.procedures.PointDefiningModeProcedure;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ParticleUpdateHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            PointDefiningModeProcedure.activeModes.values().forEach(PointDefiningModeProcedure::updateParticles);
        }
    }
}