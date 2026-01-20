package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CreateNpcMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap<>();
    public final Player entity;
    private final ItemStackHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();

    public CreateNpcMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(NpcshopkeeperModMenus.CREATE_NPC.get(), id);
        this.entity = inv.player;
        this.internal = new ItemStackHandler(0); // Pas de slots d'inventaire nécessaires

        // Pas d'initialisation de slots car c'est une GUI purement textuelle/boutons
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
