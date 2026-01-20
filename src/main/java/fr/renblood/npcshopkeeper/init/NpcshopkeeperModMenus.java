package fr.renblood.npcshopkeeper.init;
/*
 *	MCreator note: This file will be REGENERATED on each build.
 */


import com.mojang.brigadier.context.CommandContext;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.world.inventory.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.common.extensions.IForgeMenuType;

import net.minecraft.world.inventory.MenuType;

import static net.minecraftforge.registries.ForgeRegistries.Keys.MENU_TYPES;


public class NpcshopkeeperModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Npcshopkeeper.MODID);
	public static final RegistryObject<MenuType<CreateTradeMenu>> CREATE_TRADE = REGISTRY.register("create_trade", () -> IForgeMenuType.create(CreateTradeMenu::new));
	public static final RegistryObject<MenuType<fr.renblood.npcshopkeeper.world.inventory.TradeMenu>> TRADE = REGISTRY.register("trade", () -> IForgeMenuType.create((id, inv, extraData) -> new TradeMenu(id, inv, extraData)));
	public static final RegistryObject<MenuType<CategoryMenu>> CATEGORY_MENU = REGISTRY.register("category_menu",
			() -> IForgeMenuType.create((windowId, inv, data) -> new CategoryMenu(windowId, inv)));
	public static final RegistryObject<MenuType<CategoryMenu>> CATEGORY = REGISTRY.register("category",
			() -> IForgeMenuType.create((windowId, inv, data) -> new CategoryMenu(windowId, inv)));
	public static final RegistryObject<MenuType<TradeListMenu>> TRADE_LIST_MENU =
			REGISTRY.register("trade_list_menu", () ->
					IForgeMenuType.create(TradeListMenu::new
					)
			);
	public static final RegistryObject<MenuType<NpcShopkeeperWandGuiMenu>> NPC_SHOPKEEPER_WAND_GUI = REGISTRY.register("npc_shopkeeper_wand_gui", () -> IForgeMenuType.create(NpcShopkeeperWandGuiMenu::new));
	public static final RegistryObject<MenuType<SeeRoadsMenu>> SEE_ROADS = REGISTRY.register("see_roads", () -> IForgeMenuType.create(SeeRoadsMenu::new));
	public static final RegistryObject<MenuType<RoadDetailsMenu>> ROAD_DETAILS = REGISTRY.register("road_details", () -> IForgeMenuType.create((id, inv, extraData) -> new RoadDetailsMenu(id, inv, extraData)));
	public static final RegistryObject<MenuType<CreateNpcMenu>> CREATE_NPC = REGISTRY.register("create_npc", () -> IForgeMenuType.create((id, inv, extraData) -> new CreateNpcMenu(id, inv, extraData)));

}
