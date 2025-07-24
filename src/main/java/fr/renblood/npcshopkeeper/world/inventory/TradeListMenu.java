package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH;

public class TradeListMenu extends AbstractContainerMenu {

    public static final MenuType<TradeListMenu> TRADE_LIST_MENU = null;
    private final List<Trade> trades;


    // Constructor
    public TradeListMenu(int id, Inventory playerInventory, String category) {
        super(TRADE_LIST_MENU, id);

        // Stocker le trade si nécessaire (par exemple, pour afficher les détails du trade)

        JsonRepository<Trade> repo;
        repo = new JsonRepository<>(
                Paths.get(PATH),
                "trades",
                Trade::fromJson,
                Trade::toJson
        );
        this.trades = loadTrades(category);
        // Ajouter les slots de l'inventaire du joueur (comme avant)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
    public TradeListMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, extraData.readUtf(32767)); // lit la catégorie envoyée
    }
    private List<Trade> loadTrades(String category) {
        JsonRepository<Trade> repo = new JsonRepository<>(
                Paths.get(PATH),
                "trades",
                Trade::fromJson,
                Trade::toJson
        );
        return repo.loadAll()
                .stream()
                .filter(t -> t.getCategory().equalsIgnoreCase(category))
                .toList();
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

    // Check if the player can interact with the container
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    public List<Trade> getTrades() {
        return trades;
    }


}
