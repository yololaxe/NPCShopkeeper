package fr.renblood.npcshopkeeper.procedures;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.function.Supplier;

public class FinalTradeProcedure {
	public static void execute(Entity entity) {

		if (entity == null)
			return;



		if (isSlotEmpty(entity, 8)) {
			if (isSlotEmpty(entity, 9)) {
				if (isSlotEmpty(entity, 10)) {
					if (isSlotEmpty(entity, 11)) {
						if (entity instanceof ServerPlayer _player) {
							_player.displayClientMessage(Component.literal("All slots empty, closing..."), false);
							_player.closeContainer();
						}
					}
				}
			}
		}
	}

	// Méthode utilitaire pour vérifier si un slot est vide
	private static boolean isSlotEmpty(Entity entity, int sltid) {
		if (entity instanceof ServerPlayer _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
			ItemStack stack = ((Slot) _slots.get(sltid)).getItem();
			_player.displayClientMessage(Component.literal("Check slot " + sltid + ": " + stack.getDisplayName().getString()), false);
			// Vérifie si le slot est vide
			return stack.isEmpty(); // Vérifie si le stack est vide au lieu d'utiliser stack.getCount()
		}
		return false;
	}
}
