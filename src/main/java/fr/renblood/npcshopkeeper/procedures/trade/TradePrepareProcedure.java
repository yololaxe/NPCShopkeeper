package fr.renblood.npcshopkeeper.procedures.trade;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager.*;
import static fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager.findReferenceByItem;


public class TradePrepareProcedure {
	public static void execute(Entity entity, String cleanTradeName, String npcId, String npcName) {
		if (entity == null) return;

		// Check if the entity is a player
		if (entity instanceof ServerPlayer _player) {

			// Check if the containerMenu exists
			if (_player.containerMenu != null) {

				// Check if containerMenu is an instance of TradeMenu or any expected class
				if (_player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					Trade trade = getTradeByName(cleanTradeName);
					Pair<Boolean, TradeHistory> checkAndHistory = checkTradeStatusForNpc(npcId);
					String id = checkAndHistory.first ? checkAndHistory.second.getId() : UUID.randomUUID().toString();
					TradeHistory tradeHistory = checkAndHistory.second;
					boolean checkExist = checkAndHistory.first;

					final int[] slotID = {0};
					List<Map<String, Object>> tradeItems = new ArrayList<>(); // Store trade data
					Random random = new Random();

					// Retrieve trade items if the trade already exists
					List<Map<String, Object>> existingTradeItems = checkExist ? tradeHistory.getTradeItems() : null;

					trade.getTrades().forEach(tradeItem -> {
						int priceTradeItem;
						int quantity;
						ResourceLocation itemResource = new ResourceLocation(tradeItem.getItem());
						Item item = BuiltInRegistries.ITEM.get(itemResource);
						ItemStack _setstack = new ItemStack(item).copy();

						// Calculate price and quantity
						if (!checkExist) {
							Pair<Integer, Integer> minMax = findReferenceByItem(tradeItem.getItem(), _player);
							priceTradeItem = random.nextInt(minMax.second - minMax.first + 1) + minMax.first;
							quantity = tradeItem.getMin() + random.nextInt(tradeItem.getMax() - tradeItem.getMin() + 1);
						} else {
							priceTradeItem = (int) existingTradeItems.get(slotID[0] / 2).get("price");
							quantity = (int) existingTradeItems.get(slotID[0] / 2).get("quantity");
						}

						// Set the item in the slot
						_setstack.setCount(quantity);
						((Slot) _slots.get(slotID[0])).set(_setstack);

						// Add trade item to the list
						Map<String, Object> tradeMap = new HashMap<>();
						tradeMap.put("price", priceTradeItem);
						tradeMap.put("item", tradeItem.getItem());
						tradeMap.put("quantity", quantity);
						tradeItems.add(tradeMap);

						slotID[0] += 2;
					});

					// Set the trade category in slot 12
					ResourceLocation categoryResource = new ResourceLocation(trade.getCategory());
					Item categoryItem = BuiltInRegistries.ITEM.get(categoryResource);
					ItemStack categorySetStack = new ItemStack(categoryItem).copy();
					categorySetStack.setHoverName(Component.literal(trade.getName() + " " + id));
					((Slot) _slots.get(12)).set(categorySetStack);

					// Set the coins in the slots
					int totalMoneyInCopper = checkExist ? tradeHistory.getTotalPrice() : MoneyCalculator.calculateTotalMoneyFromTrade(tradeItems);
					int[] coins = MoneyCalculator.getIntInCoins(totalMoneyInCopper);

					Item goldCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:gold_coin"));
					Item silverCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:silver_coin"));
					Item bronzeCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:bronze_coin"));
					Item copperCoin = Items.COPPER_INGOT;

					ItemStack[] coinStacks = {
							new ItemStack(goldCoin, coins[0]), // Gold
							new ItemStack(silverCoin, coins[1]), // Silver
							new ItemStack(bronzeCoin, coins[2]), // Bronze
							new ItemStack(copperCoin, coins[3]) // Copper
					};

					int slotIndex = 13;
					for (int i = 0; i < coinStacks.length && slotIndex <= 14; i++) {
						if (coinStacks[i].getCount() > 0) {
							((Slot) _slots.get(slotIndex)).set(coinStacks[i]);
							slotIndex++;
						}
					}

					// Log trade start if it's a new trade
					if (!checkExist) {
						_player.displayClientMessage(Component.literal("Début de l'enregistrement dans History"), false);
						ArrayList<Player> players = new ArrayList<>(); // Append new player
						if (!players.contains(_player)) players.add(_player);
						JsonTradeFileManager.logTradeStart(_player, cleanTradeName, id, tradeItems, totalMoneyInCopper, players, npcId, npcName);
					}

				} else {
					_player.displayClientMessage(Component.literal("containerMenu is not an instance of TradeMenu"), false);
				}
			} else {
				_player.displayClientMessage(Component.literal("containerMenu is null"), false);
			}
		}
	}
}
