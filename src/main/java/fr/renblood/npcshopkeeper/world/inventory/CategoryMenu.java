package fr.renblood.npcshopkeeper.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.registries.ObjectHolder;

public class CategoryMenu extends AbstractContainerMenu {

    @ObjectHolder("yourmodid:category")
    public static final MenuType<CategoryMenu> CATEGORY_MENU = null;

    // Constructor for CategoryMenu
    public CategoryMenu(int id, Inventory playerInventory) {
        super(CATEGORY_MENU, id);

        // Here, you can add slots for player inventory if needed
        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // This checks if the player can still use the container
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
