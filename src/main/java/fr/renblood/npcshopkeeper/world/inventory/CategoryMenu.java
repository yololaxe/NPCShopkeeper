package fr.renblood.npcshopkeeper.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ObjectHolder;


public class CategoryMenu extends AbstractContainerMenu {

    public static final MenuType<CategoryMenu> CATEGORY_MENU = null;

    // Constructor
    public CategoryMenu(int id, Inventory playerInventory) {
        super(CATEGORY_MENU, id);

        // Add player inventory slots (3 rows x 9 slots)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar slots (1 row x 9 slots)
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // Handle quick move logic (Shift-clicking an item)
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            // If the slot is in the container, move it to the player's inventory
            if (index < 36) { // 36 = total number of slots in player inventory
                if (!this.moveItemStackTo(stackInSlot, 36, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // If the slot is in the player's inventory, move it to the container
                if (!this.moveItemStackTo(stackInSlot, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    // Check if the player can still use the container
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}