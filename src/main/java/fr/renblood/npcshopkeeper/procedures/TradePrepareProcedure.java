package fr.renblood.npcshopkeeper.procedures;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.data.Trade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.manager.JsonTradeFileManager.getTradeByName;
import static fr.renblood.npcshopkeeper.manager.PriceReferenceManager.findReferenceByItem;


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
					String id = UUID.randomUUID().toString();


					final int[] slotID = {0};
					List<Map<String, Object>> tradeTripleList = new ArrayList<>(); //Permet de stocké les données

					trade.getTrades().forEach(tradeItem -> {
						//PRICE REF
						Pair<Integer, Integer> minMax = findReferenceByItem(tradeItem.getItem(),_player);
						int min = minMax.first;
						int max = minMax.second;

						// Générer un nombre aléatoire entre min et max
						int priceTradeItem = new Random().nextInt(max - min + 1) + min;

						//CREATE THE SLOT
						ResourceLocation itemResource = new ResourceLocation(tradeItem.getItem());
						Item item = BuiltInRegistries.ITEM.get(itemResource);
						ItemStack _setstack = new ItemStack(item).copy();
						Random random = new Random();
						int randomCount = tradeItem.getMin() + random.nextInt(tradeItem.getMax() - tradeItem.getMin() + 1);
						_setstack.setCount(randomCount);
						((Slot) _slots.get(slotID[0])).set(_setstack);

						//CREATION DU STOCKAGE POUR LENVOYEE SUR LE JSON
						Map<String, Object> tradeMap = new HashMap<>();
						tradeMap.put("price", priceTradeItem);
						tradeMap.put("item", tradeItem.getItem());
						tradeMap.put("quantity", randomCount);

						// Ajouter le dictionnaire à la liste
						tradeTripleList.add(tradeMap);

						slotID[0]+=2;
					});

					//START
					JsonTradeFileManager.logTradeStart(_player, cleanTradeName, id, tradeTripleList);
					//START

					ResourceLocation categotyRessource = new ResourceLocation(trade.getCategory());
					Item categoryItem = BuiltInRegistries.ITEM.get(categotyRessource);
					ItemStack categorySetStack = new ItemStack(categoryItem).copy();
					categorySetStack.setHoverName(Component.literal(trade.getName()+ " " + id));

					((Slot) _slots.get(12)).set(categorySetStack);

				} else {
					_player.displayClientMessage(Component.literal("containerMenu is not an instance of TradeMenu"), false);
				}
			} else {
				_player.displayClientMessage(Component.literal("containerMenu is null"), false);
			}
		}
	}
}
