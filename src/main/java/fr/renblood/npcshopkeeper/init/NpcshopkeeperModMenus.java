package fr.renblood.npcshopkeeper.init;
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */


import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.world.inventory.CreateTradeMenu;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;


public class NpcshopkeeperModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Npcshopkeeper.MODID);
	public static final RegistryObject<MenuType<CreateTradeMenu>> CREATE_TRADE = REGISTRY.register("create_trade", () -> IForgeMenuType.create(CreateTradeMenu::new));
	public static final RegistryObject<MenuType<fr.renblood.npcshopkeeper.world.inventory.TradeMenu>> TRADE = REGISTRY.register("trade", () -> IForgeMenuType.create(TradeMenu::new));
}
