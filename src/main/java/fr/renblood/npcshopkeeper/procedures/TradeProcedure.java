package fr.renblood.npcshopkeeper.procedures;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;
import java.util.Map;

public class TradeProcedure {

	private static boolean isSlotChanging = false; // Flag pour bloquer la récursion

	public static void execute(Entity entity) {
		if (entity == null || isSlotChanging) // Si une modification de slot est déjà en cours, retourne immédiatement
			return;

		isSlotChanging = true; // Verrouiller la modification de slot pour éviter la récursion

		// Méthode utilitaire pour récupérer la quantité d'item dans un slot


		// Première comparaison et modification des slots
		if ((entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY).getItem() != Items.AIR
				&& (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(1)).getItem() : ItemStack.EMPTY).getItem() != Items.AIR
				&& (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(0)).getItem() : ItemStack.EMPTY)
				.getItem() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(1)).getItem() : ItemStack.EMPTY).getItem()
				&& getAmount(entity, 0) <= getAmount(entity, 1)) {

			// Deuxième comparaison et modification des slots
			if ((entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(2)).getItem() : ItemStack.EMPTY)
					.getItem() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(3)).getItem() : ItemStack.EMPTY).getItem()
					&& getAmount(entity, 2) <= getAmount(entity, 3)) {

				// Troisième comparaison et modification des slots
				if ((entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(4)).getItem() : ItemStack.EMPTY)
						.getItem() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(5)).getItem() : ItemStack.EMPTY).getItem()
						&& getAmount(entity, 4) <= getAmount(entity, 5)) {

					// Quatrième comparaison et modification des slots
					if ((entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(6)).getItem() : ItemStack.EMPTY)
							.getItem() == (entity instanceof Player _plrSlotItem && _plrSlotItem.containerMenu instanceof Supplier _splr && _splr.get() instanceof Map _slt ? ((Slot) _slt.get(7)).getItem() : ItemStack.EMPTY).getItem()
							&& getAmount(entity, 6) <= getAmount(entity, 7)) {

						// Effacement des slots après validation
						clearSlot(entity, 0);
						clearSlot(entity, 2);
						clearSlot(entity, 4);
						clearSlot(entity, 6);

						// Attribution des nouvelles valeurs aux slots
						setSlot(entity, 8, new ItemStack(Items.GOLD_INGOT), 1);
						setSlot(entity, 9, new ItemStack(Items.GOLD_NUGGET), 1);
						setSlot(entity, 10, new ItemStack(Items.IRON_SWORD), 1);
						setSlot(entity, 11, new ItemStack(Items.LAPIS_LAZULI), 1);
					}
				}
			}
		}

		isSlotChanging = false; // Libérer le flag après modification des slots
	}

	// Méthode utilitaire pour effacer le contenu d'un slot
	private static void clearSlot(Entity entity, int slotId) {
		if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
			((Slot) _slots.get(slotId)).set(ItemStack.EMPTY);
			_player.containerMenu.broadcastChanges();
		}
	}

	// Méthode utilitaire pour définir un item dans un slot
	private static void setSlot(Entity entity, int slotId, ItemStack stack, int count) {
		if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
			ItemStack _setstack = stack.copy();
			_setstack.setCount(count);
			((Slot) _slots.get(slotId)).set(_setstack);
			_player.containerMenu.broadcastChanges();
		}
	}
	private static int getAmount(Entity entity, int slotId) {
		if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
			ItemStack stack = ((Slot) _slots.get(slotId)).getItem();
			if (stack != null) {
				return stack.getCount();
			}
		}
		return 0;
	}
}
