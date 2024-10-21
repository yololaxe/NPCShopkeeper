package fr.renblood.npcshopkeeper.init;
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */

import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import fr.renblood.npcshopkeeper.client.gui.TradeScreen;

import net.minecraft.client.gui.screens.MenuScreens;
import fr.renblood.npcshopkeeper.client.gui.CreateTradeScreen;


@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NpcshopkeeperModScreens {
	@SubscribeEvent
	public static void clientLoad(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(NpcshopkeeperModMenus.CREATE_TRADE.get(), CreateTradeScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.TRADE.get(), TradeScreen::new);
		});
	}
}
