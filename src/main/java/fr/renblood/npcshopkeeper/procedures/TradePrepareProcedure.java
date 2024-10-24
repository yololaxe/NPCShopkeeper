package fr.renblood.npcshopkeeper.procedures;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.TradeHistory;
import fr.renblood.npcshopkeeper.data.TradeResult;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.data.Trade;
import fr.renblood.npcshopkeeper.manager.MoneyCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.manager.JsonTradeFileManager.*;
import static fr.renblood.npcshopkeeper.manager.PriceReferenceManager.findReferenceByItem;


public class TradePrepareProcedure {
	public static void execute(Entity entity, String cleanTradeName) {
		if (entity == null) return;

		// Check if the entity is a player
		if (entity instanceof ServerPlayer _player) {

			// Check if the containerMenu exists
			if (_player.containerMenu != null) {

				// Check if containerMenu is an instance of TradeMenu or any expected class
				if (_player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					Trade trade = getTradeByName(cleanTradeName);
					Pair<Boolean, TradeHistory> checkAndHistory = checkTradeStatusForPlayer(_player, cleanTradeName);
					String id = checkAndHistory.first ? checkAndHistory.second.getId() : UUID.randomUUID().toString();
					TradeHistory tradeHistory = checkAndHistory.second;
					boolean checkExist = checkAndHistory.first;

					final int[] slotID = {0};
					List<Map<String, Object>> tradeItems = new ArrayList<>(); // Stockage des données
					Random random = new Random();

					// Optimisation : Récupération anticipée des tradeItems
					List<Map<String, Object>> existingTradeItems = checkExist ? tradeHistory.getTradeItems() : null;

					trade.getTrades().forEach(tradeItem -> {
						int priceTradeItem;
						int quantity;
						ResourceLocation itemResource = new ResourceLocation(tradeItem.getItem());
						Item item = BuiltInRegistries.ITEM.get(itemResource);
						ItemStack _setstack = new ItemStack(item).copy();

						// Calcul du prix et de la quantité
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

						// Ajouter le trade item dans la liste
						Map<String, Object> tradeMap = new HashMap<>();
						tradeMap.put("price", priceTradeItem);
						tradeMap.put("item", tradeItem.getItem());
						tradeMap.put("quantity", quantity);
						tradeItems.add(tradeMap);

						slotID[0] += 2;
					});

					// Set the trade category in slot 12
					ResourceLocation categotyRessource = new ResourceLocation(trade.getCategory());
					Item categoryItem = BuiltInRegistries.ITEM.get(categotyRessource);
					ItemStack categorySetStack = new ItemStack(categoryItem).copy();
					categorySetStack.setHoverName(Component.literal(trade.getName() + " " + id));
					((Slot) _slots.get(12)).set(categorySetStack);

					// PARTIE POUR SET LES COINS
					int totalMoneyInCopper = checkExist ? tradeHistory.getTotalPrice() : MoneyCalculator.calculateTotalMoneyFromTrade(tradeItems);
					int[] coins = MoneyCalculator.getIntInCoins(totalMoneyInCopper);

					// Optimisation : Déclaration des items de pièce de monnaie
					Item goldCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:gold_coin"));
					Item silverCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:silver_coin"));
					Item bronzeCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:bronze_coin"));
					Item copperCoin = Items.COPPER_INGOT;

					// Optimisation : Création des stacks de pièces
					ItemStack[] coinStacks = {
							new ItemStack(goldCoin, coins[0]),    // Or
							new ItemStack(silverCoin, coins[1]),  // Argent
							new ItemStack(bronzeCoin, coins[2]),  // Bronze
							new ItemStack(copperCoin, coins[3])   // Cuivre
					};

					// Place the two highest denominations in slots 13 and 14
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
						JsonTradeFileManager.logTradeStart(_player, cleanTradeName, id, tradeItems, totalMoneyInCopper);
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

