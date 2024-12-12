package fr.renblood.npcshopkeeper.init;
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.item.NpcShopkeerperWandItem;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.item.Item;


public class NpcshopkeeperModItems {
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(ForgeRegistries.ITEMS, Npcshopkeeper.MODID);
	public static final RegistryObject<Item> NPC_SHOPKEEPER_WAND = REGISTRY.register("npc_shopkeeper_wand", () -> new NpcShopkeerperWandItem());
	// Start of user code block custom items
	// End of user code block custom items
}
