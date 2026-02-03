package fr.renblood.npcshopkeeper.events;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.SyncGlobalNpcDataPacket;
import fr.renblood.npcshopkeeper.network.SyncHarborConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Synchroniser les données globales des PNJs avec le client
            PacketHandler.sendToPlayer(new SyncGlobalNpcDataPacket(GlobalNpcManager.getAllNpcData()), player);
            
            // Synchroniser la configuration du port (prix et durée du jour)
            PacketHandler.sendToPlayer(new SyncHarborConfigPacket(PortManager.getBlocksPerIron(), PortManager.getDayLengthInMinutes()), player);
        }
    }
}
