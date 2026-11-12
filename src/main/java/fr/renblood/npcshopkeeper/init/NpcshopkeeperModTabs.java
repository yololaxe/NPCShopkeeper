package fr.renblood.npcshopkeeper.init;
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;



@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NpcshopkeeperModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Npcshopkeeper.MODID);
	public static final RegistryObject<CreativeModeTab> NPC_SHOPKEEPER_TAB = REGISTRY.register("npc_shopkeeper", () ->
			CreativeModeTab.builder()
					.title(Component.translatable("itemGroup.tab_npc_shopkeeper"))
					.icon(() -> NpcshopkeeperModItems.NPC_SHOPKEEPER_WAND.get().getDefaultInstance())
					.displayItems((parameters, output) -> {output.accept(NpcshopkeeperModItems.NPC_SHOPKEEPER_WAND.get());output.accept(NpcshopkeeperModItems.NPC_SHOP_WAND.get());})
					.build());

}
