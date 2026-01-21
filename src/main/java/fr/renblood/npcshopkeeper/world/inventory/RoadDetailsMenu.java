package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class RoadDetailsMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap<>();
    public final Player entity;
    private final ItemStackHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    public final CommercialRoad road;

    public RoadDetailsMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(NpcshopkeeperModMenus.ROAD_DETAILS.get(), id);
        this.entity = inv.player;
        String roadId = extraData.readUtf();
        this.road = Npcshopkeeper.COMMERCIAL_ROADS.stream()
                .filter(r -> r.getId().equals(roadId))
                .findFirst()
                .orElse(null);

        this.internal = new ItemStackHandler(27); // Assez de place pour les PNJs

        init();
    }

    private void init() {
        if (road == null) return;

        int slotIndex = 0;
        for (TradeNpcEntity npc : road.getNpcEntities()) {
            if (slotIndex >= internal.getSlots()) break;
            
            ItemStack npcStack = new ItemStack(Items.PLAYER_HEAD);
            npcStack.setHoverName(Component.literal(npc.getNpcName()));
            
            // Gestion du skin sur la tête
            String texture = npc.getTradeNpc().getTexture();
            if (texture != null && !texture.startsWith("textures/")) {
                // C'est un pseudo (PNJ créé par joueur)
                CompoundTag tag = npcStack.getOrCreateTag();
                tag.putString("SkullOwner", texture); // Minecraft chargera le skin automatiquement
            } else {
                // C'est une texture locale, on ne peut pas l'afficher facilement sur un item
                // On laisse la tête de Steve par défaut, mais avec le bon nom
            }
            
            internal.setStackInSlot(slotIndex++, npcStack);
        }

        // Slots pour les PNJs
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9;
                this.customSlots.put(index, this.addSlot(new SlotItemHandler(internal, index, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                }));
            }
        }
        
        // Inventaire joueur (pour l'affichage standard)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(entity.getInventory(), col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(entity.getInventory(), col, 8 + col * 18, 142));
        }
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
