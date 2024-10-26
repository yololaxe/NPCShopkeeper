package fr.renblood.npcshopkeeper.init;
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */


import com.mojang.brigadier.context.CommandContext;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.world.inventory.CategoryMenu;
import fr.renblood.npcshopkeeper.world.inventory.CreateTradeMenu;
import fr.renblood.npcshopkeeper.world.inventory.TradeListMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;

import static net.minecraftforge.registries.ForgeRegistries.Keys.MENU_TYPES;


public class NpcshopkeeperModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Npcshopkeeper.MODID);
	public static final RegistryObject<MenuType<CreateTradeMenu>> CREATE_TRADE = REGISTRY.register("create_trade", () -> IForgeMenuType.create(CreateTradeMenu::new));
	public static final RegistryObject<MenuType<fr.renblood.npcshopkeeper.world.inventory.TradeMenu>> TRADE = REGISTRY.register("trade", () -> IForgeMenuType.create((id, inv, extraData) -> new TradeMenu(id, inv, extraData)));
	public static final RegistryObject<MenuType<CategoryMenu>> CATEGORY_MENU = REGISTRY.register("category_menu",
			() -> IForgeMenuType.create((windowId, inv, data) -> new CategoryMenu(windowId, inv)));

	public static final RegistryObject<MenuType<TradeListMenu>> TRADE_LIST_MENU = REGISTRY.register("trade_list_menu",
			() -> IForgeMenuType.create((windowId, inv, data) -> new TradeListMenu(windowId, inv)));

}
