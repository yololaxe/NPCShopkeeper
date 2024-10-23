package fr.renblood.npcshopkeeper.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;
import java.util.Map;

import static fr.renblood.npcshopkeeper.data.JsonTradeFileManager.markTradeAsFinished;

public class TradeProcedure {

	private static boolean isProcessingTrade = false; // Flag pour éviter les appels multiples

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

			// Comparer et modifier les slots étape par étape
			if (isValidSlotPair(_slots, 0, 1, player) &&
					isValidSlotPair(_slots, 2, 3, player) &&
					isValidSlotPair(_slots, 4, 5, player) &&
					isValidSlotPair(_slots, 6, 7, player)) {


				clearAndRemoveSlots(player, _slots);
				giveRewards(player, _slots);
				markTradeAsFinished(player, ((Slot) _slots.get(12)).getItem().getDisplayName().getString());
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
		player.displayClientMessage(Component.literal("On commence le CLEAR"), false);
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
	private static void giveRewards(ServerPlayer player, Map _slots) {
		setSlot(_slots, 8, new ItemStack(Items.GOLD_INGOT), 1);
		setSlot(_slots, 9, new ItemStack(Items.GOLD_NUGGET), 1);
		setSlot(_slots, 10, new ItemStack(Items.IRON_SWORD), 1);
		setSlot(_slots, 11, new ItemStack(Items.LAPIS_LAZULI), 1);
		player.containerMenu.broadcastChanges();
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
