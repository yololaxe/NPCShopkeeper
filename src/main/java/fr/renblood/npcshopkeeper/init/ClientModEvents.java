package fr.renblood.npcshopkeeper.init;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.client.renderer.TradeNpcRenderer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static fr.renblood.npcshopkeeper.init.EntityInit.TRADE_NPC_ENTITY;

@Mod.EventBusSubscriber(modid = Npcshopkeeper.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(
                    TRADE_NPC_ENTITY.get(),
                    TradeNpcRenderer::new
            );
        });
    }
}
