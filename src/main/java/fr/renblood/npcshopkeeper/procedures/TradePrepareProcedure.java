package fr.renblood.npcshopkeeper.procedures;

import com.mojang.brigadier.context.CommandContext;
import fr.renblood.npcshopkeeper.data.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.data.Trade;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.data.JsonTradeFileManager.getTradeByName;


public class TradePrepareProcedure {
	public static void execute(Entity entity, String cleanTradeName) {
		if (entity == null)
			return;

		// Check if the entity is a player
		if (entity instanceof ServerPlayer _player) {
			//_player.displayClientMessage(Component.literal("Entity is a Player"), false);

			// Check if the containerMenu exists
			if (_player.containerMenu != null) {
				//_player.displayClientMessage(Component.literal("containerMenu exists, Type: " + _player.containerMenu.slots), false);

				// Check if containerMenu is an instance of TradeMenu or any expected class
				if (_player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					//_player.displayClientMessage(Component.literal("containerMenu is an instance of TradeMenu"), false);
					//String cleanTradeName = arguments.getInput().substring(6); // Enlève le trade au début
					Trade trade = getTradeByName(cleanTradeName);
					JsonTradeFileManager.logTradeStart(_player, cleanTradeName);

					ResourceLocation categotyRessource = new ResourceLocation(trade.getCategory());
					Item categoryItem = BuiltInRegistries.ITEM.get(categotyRessource);
					ItemStack categorySetStack = new ItemStack(categoryItem).copy();
					categorySetStack.setHoverName(Component.literal(trade.getName()));

					((Slot) _slots.get(12)).set(categorySetStack);

					final int[] slotID = {0};
					trade.getTrades().forEach(tradeItem -> {
						ResourceLocation itemResource = new ResourceLocation(tradeItem.getItem());
						Item item = BuiltInRegistries.ITEM.get(itemResource);
						ItemStack _setstack = new ItemStack(item).copy();
						Random random = new Random();
						int randomCount = tradeItem.getMin() + random.nextInt(tradeItem.getMax() - tradeItem.getMin() + 1);
						_setstack.setCount(randomCount);
						((Slot) _slots.get(slotID[0])).set(_setstack);

						slotID[0]+=2;
					});

				} else {
					_player.displayClientMessage(Component.literal("containerMenu is not an instance of TradeMenu"), false);
				}
			} else {
				_player.displayClientMessage(Component.literal("containerMenu is null"), false);
			}
		}
	}
}
