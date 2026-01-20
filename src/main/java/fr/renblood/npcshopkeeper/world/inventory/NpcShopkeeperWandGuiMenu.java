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
		this.internal = new ItemStackHandler(0); // 0 slots internes car pas d'inventaire
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

		// Initialiser les slots personnalisés (aucun ici)
		init(inv);
	}
	public String getCategory() {
		return category;
	}

	private void init(Inventory playerInventory) {
		// Pas de slots internes
		
		// Pas de slots d'inventaire joueur
		
		// Pas de barre de raccourcis
	}

	@Override
	public boolean stillValid(Player player) {
		return true; // Toujours valide car c'est juste une GUI de configuration
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public Map<Integer, Slot> get() {
		return customSlots;
	}
}
