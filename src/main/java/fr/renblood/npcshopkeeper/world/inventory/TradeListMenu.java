package fr.renblood.npcshopkeeper.world.inventory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.registries.ObjectHolder;

public class TradeListMenu extends AbstractContainerMenu {

    @ObjectHolder("yourmodid:trade_list")
    public static final MenuType<TradeListMenu> TRADE_LIST_MENU = null;

    // Constructor for TradeListMenu
    public TradeListMenu(int id, Inventory playerInventory) {
        super(TRADE_LIST_MENU, id);

        // You can add player inventory slots here if needed, just like in CategoryMenu
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

    // Check if the player can still interact with the container
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}