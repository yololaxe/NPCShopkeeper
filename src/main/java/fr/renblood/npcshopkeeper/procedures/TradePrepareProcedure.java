package fr.renblood.npcshopkeeper.procedures;

import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.function.Supplier;

public class TradePrepareProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;

		// Check if the entity is a player
		if (entity instanceof Player _player) {
			_player.displayClientMessage(Component.literal("Entity is a Player"), false);

			// Check if the containerMenu exists
			if (_player.containerMenu != null) {
				_player.displayClientMessage(Component.literal("containerMenu exists, Type: " + _player.containerMenu.slots), false);

				// Check if containerMenu is an instance of TradeMenu or any expected class
				if (_player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
					_player.displayClientMessage(Component.literal("containerMenu is an instance of TradeMenu"), false);


					// Perform the logic for setting up the slot
					ItemStack _setstack = new ItemStack(Blocks.SPRUCE_LOG).copy();
					_setstack.setCount(1);

					((Slot) _slots.get(0)).set(_setstack);
					_player.containerMenu.broadcastChanges();

					((Slot) _slots.get(2)).set(_setstack);
					_player.containerMenu.broadcastChanges();

					((Slot) _slots.get(4)).set(_setstack);
					_player.containerMenu.broadcastChanges();

					((Slot) _slots.get(6)).set(_setstack);
					_player.containerMenu.broadcastChanges();
				} else {
					_player.displayClientMessage(Component.literal("containerMenu is not an instance of TradeMenu"), false);
				}
			} else {
				_player.displayClientMessage(Component.literal("containerMenu is null"), false);
			}
		}
	}
}
