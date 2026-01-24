package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class TravelMenu extends AbstractContainerMenu {
    private final String currentPortName;

    public TravelMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(NpcshopkeeperModMenus.TRAVEL.get(), id);
        this.currentPortName = extraData.readUtf();
    }

    // Constructeur serveur
    public TravelMenu(int id, Inventory inv, String currentPortName) {
        super(NpcshopkeeperModMenus.TRAVEL.get(), id);
        this.currentPortName = currentPortName;
    }

    public String getCurrentPortName() {
        return currentPortName;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
