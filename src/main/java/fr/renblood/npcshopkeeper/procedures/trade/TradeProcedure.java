package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.trade.TradeResult;
import fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;
import java.util.Map;

import static fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager.*;


public class TradeProcedure {

	private static boolean isProcessingTrade = false; // Flag pour éviter les appels multiples
	private static final Logger LOGGER = LogManager.getLogger(JsonTradeFileManager.class);

	// Méthode principale de gestion du trade
	public static void execute(Entity entity) {
		// Si une modification de slot est déjà en cours, retourne immédiatement
		if (entity == null || isProcessingTrade)
			return;

		isProcessingTrade = true; // Verrouiller la procédure pour éviter les doublons


		// Si l'entité est un joueur avec un menu valide
		if (isServerPlayerWithMenu(entity)) {
			ServerPlayer player = (ServerPlayer) entity;

			Map _slots = getSlots(player);
			String tradeSlot = ((Slot) _slots.get(12)).getItem().getDisplayName().getString();

			tradeSlot = tradeSlot.replace("[", "").replace("]", "");
			String[] parts = tradeSlot.split(" ", 2); // Sépare la chaîne en deux parties : avant et après le premier espace
			String tradeName = parts[0];
			String tradeId = parts[1];
//			player.displayClientMessage(Component.literal("Trade"+tradeName+" : "+tradeId), false);

			TradeHistory tradeHistory = getTradeHistoryById(tradeId);
//			player.displayClientMessage(Component.literal("Trade"+tradeName+" : "+ tradeHistory.isFinished()), false);
			// Comparer et modifier les slots étape par étape
			if (isValidSlotPair(_slots, 0, 1, player) &&
					isValidSlotPair(_slots, 2, 3, player) &&
					isValidSlotPair(_slots, 4, 5, player) &&
					isValidSlotPair(_slots, 6, 7, player)
			&& !tradeHistory.isFinished()) {



				clearAndRemoveSlots(player, _slots);
				giveRewards(player, _slots, tradeId, tradeName);
				markTradeAsFinished(player, tradeId);
				player.containerMenu.broadcastChanges();
			}
		}

		isProcessingTrade = false; // Libérer le flag après modification des slots
	}

	// Méthode utilitaire pour vérifier si l'entité est un joueur avec un menu valide
	private static boolean isServerPlayerWithMenu(Entity entity) {
		return entity instanceof ServerPlayer _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map;
	}

	// Méthode utilitaire pour récupérer les slots
	private static Map getSlots(ServerPlayer player) {
		return (Map) ((Supplier) player.containerMenu).get();
	}

	// Méthode utilitaire pour valider une paire de slots
	private static boolean isValidSlotPair(Map _slots, int slotId1, int slotId2, ServerPlayer player) {
		return (getItem(_slots, slotId1).getItem() == Items.AIR &&
				getItem(_slots, 8).getItem() == Items.AIR &&
				getItem(_slots, 9).getItem() == Items.AIR &&
				getItem(_slots, 10).getItem() == Items.AIR &&
				getItem(_slots, 11).getItem() == Items.AIR )
				||
				(getItem(_slots, slotId1).getItem() != Items.AIR &&
						getItem(_slots, slotId2).getItem() != Items.AIR &&
						getItem(_slots, 8).getItem() == Items.AIR &&
						getItem(_slots, 9).getItem() == Items.AIR &&
						getItem(_slots, 10).getItem() == Items.AIR &&
						getItem(_slots, 11).getItem() == Items.AIR &&
						getItem(_slots, slotId1).getItem() == getItem(_slots, slotId2).getItem() &&
						getAmount(_slots, slotId1) <= getAmount(_slots, slotId2));
	}

	// Méthode utilitaire pour effacer les slots et mettre à jour les quantités
	private static void clearAndRemoveSlots(ServerPlayer player, Map _slots) {
		removeItem(player, _slots, 0, 1);
		removeItem(player, _slots, 2, 3);
		removeItem(player, _slots, 4, 5);
		removeItem(player, _slots, 6, 7);
		clearSlot(player, _slots, 0);
		clearSlot(player, _slots, 2);
		clearSlot(player, _slots, 4);
		clearSlot(player, _slots, 6);
		player.containerMenu.broadcastChanges();
	}

	// Méthode utilitaire pour donner les récompenses
	private static void giveRewards(ServerPlayer player, Map _slots, String tradeId, String tradeName) {
		// Récupérer l'historique du trade
		LOGGER.info("On est dans give reward");
		TradeHistory tradeHistory = getTradeHistoryById(tradeId);
		if (tradeHistory == null) {
			LOGGER.error("Aucun historique de trade trouvé pour le trade ID : " + tradeId);
			return;
		}
		LOGGER.info("Historique de trade trouvé pour le joueur : " + tradeHistory.getPlayer());


		// Calculer le total d'argent à partir des tradeItems
		int totalMoneyInCopper = tradeHistory.getTotalPrice();
		LOGGER.info("Total d'argent calculé (en cuivre) : " + totalMoneyInCopper);

		// Convertir le total en pièces (Gold, Silver, Bronze, Copper)
		int[] coins = MoneyCalculator.getIntInCoins(totalMoneyInCopper);
		LOGGER.info("Conversion des pièces : Or = " + coins[0] + ", Argent = " + coins[1] + ", Bronze = " + coins[2] + ", Cuivre = " + coins[3]);

		// Définir les items correspondants aux pièces
		Item goldCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:gold_coin"));
		Item silverCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:silver_coin"));
		Item bronzeCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:bronze_coin"));
		Item copperCoin = Items.COPPER_INGOT;

		// Tableau contenant les pièces et leur quantité
		ItemStack[] coinStacks = {
				new ItemStack(goldCoin, coins[0]),    // Or
				new ItemStack(silverCoin, coins[1]),  // Argent
				new ItemStack(bronzeCoin, coins[2]),  // Bronze
				new ItemStack(copperCoin, coins[3])   // Cuivre
		};

		// Trouver les deux types de pièces les plus hautes avec au moins une pièce
		int slotIndex = 8;
		for (int i = 0; i < coinStacks.length && slotIndex <= 9; i++) {
			if (coinStacks[i].getCount() > 0) {
				LOGGER.info("Ajout de " + coins[i] + " pièce(s) dans le slot " + slotIndex);
				setSlot(_slots, slotIndex, coinStacks[i], coins[i]);
				slotIndex++;
			}
		}

		// Récupérer le résultat du trade et placer l'item dans le slot 10
		TradeResult result = getTradeByName(tradeName).getResult();
		if (result == null) {
			LOGGER.error("Aucun résultat trouvé pour le trade : " + tradeName);
			return;
		}
		ResourceLocation itemResource = new ResourceLocation(result.getItem());
		Item item = BuiltInRegistries.ITEM.get(itemResource);
		LOGGER.info("Ajout de " + result.getQuantity() + " de " + result.getItem() + " dans le slot 10.");
		setSlot(_slots, 10, new ItemStack(item), result.getQuantity());

		// Si nécessaire, diffuser les changements de l'inventaire
		player.containerMenu.broadcastChanges();
		LOGGER.info("Changements de l'inventaire diffusés au joueur.");
	}



	// Méthode utilitaire pour effacer le contenu d'un slot
	private static void clearSlot(ServerPlayer player, Map _slots, int slotId) {
		((Slot) _slots.get(slotId)).set(ItemStack.EMPTY);
		player.containerMenu.broadcastChanges();
	}

	// Méthode utilitaire pour enlever les items d'un slot
	private static void removeItem(ServerPlayer player, Map _slots, int slotIdReq, int slotId) {
		ItemStack reqStack = ((Slot) _slots.get(slotIdReq)).getItem();
		ItemStack slotStack = ((Slot) _slots.get(slotId)).getItem();

		if (!slotStack.isEmpty() && !reqStack.isEmpty()) {
			int initialCount = slotStack.getCount();
			int reqCount = reqStack.getCount();
			int newCount = initialCount - reqCount;

			if (newCount < 0) newCount = 0;

			if (newCount > 0) {
				ItemStack updatedStack = slotStack.copy();
				updatedStack.setCount(newCount);
				((Slot) _slots.get(slotId)).set(updatedStack);
			} else {
				((Slot) _slots.get(slotId)).set(ItemStack.EMPTY);
			}

			player.containerMenu.broadcastChanges();
		}
	}

	private static void setSlot(Map _slots, int slotId, ItemStack stack, int count) {
		ItemStack _setstack = stack.copy();
		_setstack.setCount(count);
		((Slot) _slots.get(slotId)).set(_setstack);
	}

	private static ItemStack getItem(Map _slots, int slotId) {
		return _slots.containsKey(slotId) ? ((Slot) _slots.get(slotId)).getItem() : ItemStack.EMPTY;
	}

	private static int getAmount(Map _slots, int slotId) {
		ItemStack stack = getItem(_slots, slotId);
		return stack != null ? stack.getCount() : 0;
	}
}
