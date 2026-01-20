package fr.renblood.npcshopkeeper.world.inventory;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import fr.renblood.npcshopkeeper.network.SeeRoadsButtonMessage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SeeRoadsMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap<>();
    public final Player entity;
    private final ItemStackHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private final int page;
    private static final int ROADS_PER_PAGE = 27; // 3 lignes de 9
    private final List<CommercialRoad> displayedRoads;

    public SeeRoadsMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(NpcshopkeeperModMenus.SEE_ROADS.get(), id);
        this.entity = inv.player;
        this.page = extraData.readInt();
        this.internal = new ItemStackHandler(ROADS_PER_PAGE);
        this.displayedRoads = new ArrayList<>();

        init();
    }

    public int getPage() {
        return page;
    }

    private void init() {
        // 1. Récupérer et trier les routes par catégorie
        List<CommercialRoad> roads = new ArrayList<>(Npcshopkeeper.COMMERCIAL_ROADS);
        roads.sort(Comparator.comparing(CommercialRoad::getCategory).thenComparing(CommercialRoad::getName));

        int start = page * ROADS_PER_PAGE;
        int end = Math.min(start + ROADS_PER_PAGE, roads.size());

        for (int i = start; i < end; i++) {
            CommercialRoad road = roads.get(i);
            displayedRoads.add(road);
            
            // 2. Récupérer l'item correspondant à la catégorie
            Item categoryItem = Items.FILLED_MAP; // Fallback par défaut
            try {
                if (road.getCategory() != null && !road.getCategory().isEmpty()) {
                    ResourceLocation res = new ResourceLocation(road.getCategory());
                    if (BuiltInRegistries.ITEM.containsKey(res)) {
                        categoryItem = BuiltInRegistries.ITEM.get(res);
                    }
                }
            } catch (Exception e) {
                // Ignorer si la catégorie n'est pas un item valide
            }

            ItemStack stack = new ItemStack(categoryItem);
            stack.setHoverName(Component.literal(road.getName()));
            
            internal.setStackInSlot(i - start, stack);
        }

        // Slots de l'interface (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                this.customSlots.put(slotIndex, this.addSlot(new SlotItemHandler(internal, slotIndex, 8 + col * 18, 18 + row * 18) {
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
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < displayedRoads.size()) {
            CommercialRoad road = displayedRoads.get(slotId);
            if (road != null) {
                // Envoyer un packet pour ouvrir les détails de la route
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new SeeRoadsButtonMessage(road.getId()));
            }
        }
        super.clicked(slotId, dragType, clickType, player);
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
