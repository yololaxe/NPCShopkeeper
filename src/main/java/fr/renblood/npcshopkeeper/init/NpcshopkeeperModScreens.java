package fr.renblood.npcshopkeeper.init;
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
import fr.renblood.npcshopkeeper.client.gui.CategoryScreen;

import fr.renblood.npcshopkeeper.client.gui.CreateNpcScreen;
import fr.renblood.npcshopkeeper.client.gui.NpcShopkeeperWandGuiScreen;
import fr.renblood.npcshopkeeper.client.gui.RoadDetailsScreen;
import fr.renblood.npcshopkeeper.client.gui.SeeRoadsScreen;
import fr.renblood.npcshopkeeper.client.gui.TravelScreen;
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
			MenuScreens.register(NpcshopkeeperModMenus.CATEGORY.get(), CategoryScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.NPC_SHOPKEEPER_WAND_GUI.get(), NpcShopkeeperWandGuiScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.SEE_ROADS.get(), SeeRoadsScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.ROAD_DETAILS.get(), RoadDetailsScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.CREATE_NPC.get(), CreateNpcScreen::new);
			MenuScreens.register(NpcshopkeeperModMenus.TRAVEL.get(), TravelScreen::new);

		});
	}
}
