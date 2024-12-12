package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class NpcShopkeeperWandGuiMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public static final HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public final int x;
	public final int y;
	public final int z;
	private final IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private final ContainerLevelAccess access;
	private final String category;

	public NpcShopkeeperWandGuiMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(NpcshopkeeperModMenus.NPC_SHOPKEEPER_WAND_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(9); // Exemple de 9 slots internes
		BlockPos pos = null;

		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
		} else {
			this.x = this.y = this.z = 0;
		}

		this.access = pos != null ? ContainerLevelAccess.create(world, pos) : ContainerLevelAccess.NULL;
		this.category = extraData.readUtf(32767);

		// Initialiser les slots personnalisés
		init(inv);
	}
	public String getCategory() {
		return category;
	}

	private void init(Inventory playerInventory) {
		// Ajouter les slots internes (exemple de 3x3 grille)
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int slotIndex = row * 3 + col;
				int xPosition = 62 + col * 18;
				int yPosition = 17 + row * 18;
				this.customSlots.put(slotIndex, this.addSlot(new SlotItemHandler(internal, slotIndex, xPosition, yPosition)));
			}
		}

		// Ajouter les slots de l'inventaire du joueur
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				int slotIndex = col + row * 9 + 9;
				int xPosition = 8 + col * 18;
				int yPosition = 84 + row * 18;
				this.addSlot(new Slot(playerInventory, slotIndex, xPosition, yPosition));
			}
		}

		// Ajouter la barre de raccourcis
		for (int col = 0; col < 9; col++) {
			int slotIndex = col;
			int xPosition = 8 + col * 18;
			int yPosition = 142;
			this.addSlot(new Slot(playerInventory, slotIndex, xPosition, yPosition));
		}
	}

	@Override
	public boolean stillValid(Player player) {
		return this.access.evaluate((level, pos) -> player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 64, true);
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			if (index < this.internal.getSlots()) {
				if (!this.moveItemStackTo(itemstack1, this.internal.getSlots(), this.slots.size(), true)) {
					return ItemStack.EMPTY;
				}
			} else if (!this.moveItemStackTo(itemstack1, 0, this.internal.getSlots(), false)) {
				return ItemStack.EMPTY;
			}

			if (itemstack1.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}
		return itemstack;
	}

	@Override
	public Map<Integer, Slot> get() {
		return customSlots;
	}
}
