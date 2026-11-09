package fr.renblood.npcshopkeeper.api;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;
import fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.List;
import java.util.LinkedHashSet;

public final class NpcShopkeeperAPI {
    private static final Random RANDOM = new Random();
    public static final String INN_CUSTOMER_TAG = "RenbloodInnCustomer";
    public static final String INN_NAME_TAG = "RenbloodInnName";
    public static final String REQUEST_ITEM_ID_TAG = "RenbloodInnRequestItemId";
    public static final String REQUEST_ITEM_NAME_TAG = "RenbloodInnRequestItemName";
    public static final String REQUEST_PRICE_TAG = "RenbloodInnRequestPrice";
    public static final String REQUEST_TYPE_TAG = "RenbloodInnRequestType";
    public static final String FINISHED_TAG = "RenbloodInnCustomerFinished";

    private NpcShopkeeperAPI() {
    }

    public static boolean hasCommercialRoute(String routeId) {
        return findInnMenuRoute(routeId).isPresent();
    }

    public static String resolveInnMenuRouteId(String selector) {
        return findInnMenuRoute(selector).map(CommercialRoad::getId).orElse("");
    }

    public static List<String> getInnMenuRouteSelectors() {
        List<CommercialRoad> routes = innMenuRoutes();
        java.util.ArrayList<String> selectors = new java.util.ArrayList<>();
        for (int index = 0; index < routes.size(); index++) {
            selectors.add(Integer.toString(index));
            selectors.add(routes.get(index).getName());
        }
        return selectors;
    }

    public static List<String> getTradeItemIds() {
        LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        RuntimeDataCache.getTrades().stream()
                .sorted(java.util.Comparator.comparing(trade -> trade.getName() == null ? "" : trade.getName(), String.CASE_INSENSITIVE_ORDER))
                .forEach(trade -> {
                    if (trade.getTrades() != null) {
                        trade.getTrades().stream().map(item -> item.getItem())
                                .filter(item -> item != null && !item.isBlank()).forEach(itemIds::add);
                    }
                    if (trade.getResult() != null && trade.getResult().getItem() != null
                            && !trade.getResult().getItem().isBlank()) {
                        itemIds.add(trade.getResult().getItem());
                    }
                });
        return List.copyOf(itemIds);
    }

    public static List<String> getMissingTradeReferenceItemIds() {
        return getTradeItemIds().stream()
                .filter(itemId -> MedievalCoinsIntegration.getReferencePrice(itemId).orElse(0) <= 0)
                .toList();
    }

    public static long[] suggestTradeReferenceRange(ServerPlayer player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank()) return new long[]{0L, 0L};
        ResourceLocation targetId = ResourceLocation.tryParse(itemId);
        if (targetId == null) return new long[]{0L, 0L};

        long bestUnitCost = Long.MAX_VALUE;
        for (CraftingRecipe recipe : player.getServer().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = recipe.getResultItem(player.getServer().registryAccess());
            if (result.isEmpty() || !targetId.equals(ForgeRegistries.ITEMS.getKey(result.getItem()))) continue;
            long ingredientCost = 0L;
            boolean complete = true;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty()) continue;
                long cheapest = Long.MAX_VALUE;
                for (ItemStack option : ingredient.getItems()) {
                    ResourceLocation optionId = ForgeRegistries.ITEMS.getKey(option.getItem());
                    if (optionId == null) continue;
                    int price = MedievalCoinsIntegration.getReferencePrice(optionId.toString()).orElse(0);
                    if (price > 0) cheapest = Math.min(cheapest, price);
                }
                if (cheapest == Long.MAX_VALUE) {
                    complete = false;
                    break;
                }
                ingredientCost += cheapest;
            }
            if (complete && ingredientCost > 0) {
                bestUnitCost = Math.min(bestUnitCost, Math.max(1L, ingredientCost / Math.max(1, result.getCount())));
            }
        }
        if (bestUnitCost == Long.MAX_VALUE) return new long[]{0L, 0L};
        return new long[]{Math.max(1L, Math.round(bestUnitCost * 0.8D)), Math.max(1L, Math.round(bestUnitCost * 1.2D))};
    }

    public static boolean createTradePriceReference(ServerPlayer player, String itemId, int minPrice, int maxPrice,
                                                     boolean sendToBackend) {
        if (player == null || !player.hasPermissions(2) || minPrice < 1 || maxPrice < minPrice) return false;
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null || !ForgeRegistries.ITEMS.containsKey(id)) return false;
        ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(id));
        if (!PriceReferenceManager.createPriceReference(itemId, minPrice, maxPrice, player)) return false;
        if (sendToBackend) {
            player.sendSystemMessage(Component.literal("Synchronisation MedievalCoins et actualisation du catalogue..."));
            String displayName = stack.isEmpty() ? itemId : stack.getHoverName().getString();
            String category = stack.isEdible() ? "FOOD" : "OTHER";
            long referencePrice = Math.max(1L, Math.round((minPrice + (double) maxPrice) / 2.0D));
            MedievalCoinsIntegration.upsertPriceReference(itemId, displayName, category, referencePrice, minPrice, maxPrice)
                    .thenAccept(synced -> player.getServer().execute(() -> player.sendSystemMessage(Component.literal(synced
                            ? "Reference synchronisee. Le catalogue MedievalCoins est actualise."
                            : "Reference locale enregistree, mais synchronisation backend echouee."))));
        } else {
            player.sendSystemMessage(Component.literal("Reference enregistree localement uniquement."));
        }
        return true;
    }

    public static java.util.List<String> getCommercialRouteSummaries() {
        List<CommercialRoad> routes = innMenuRoutes();
        java.util.ArrayList<String> summaries = new java.util.ArrayList<>();
        for (int index = 0; index < routes.size(); index++) {
            CommercialRoad road = routes.get(index);
            summaries.add(index + " | " + road.getName() + " | type=MENU_AUBERGE | points=" + road.getPositions().size()
                    + " | timing=" + road.getMinTimer() + "-" + road.getMaxTimer() + "s");
        }
        return summaries;
    }

    public static int clearInnCustomers(ServerLevel level, String innName) {
        if (level == null || innName == null || innName.isBlank()) return 0;
        var customers = NpcEntityIndex.innCustomers(level, innName);
        for (TradeNpcEntity npc : customers) {
            GlobalNpcManager.deactivateNpc(npc.getNpcName());
            npc.discard();
        }
        return customers.size();
    }

    public static SpawnResult spawnInnCustomer(ServerLevel level, String routeId, InnCustomerRequest request) {
        if (level == null || request == null || routeId == null || routeId.isBlank()) {
            return SpawnResult.INVALID;
        }

        Optional<CommercialRoad> routeOpt = findInnMenuRoute(routeId);
        if (routeOpt.isEmpty()) return SpawnResult.NO_ROUTE;

        CommercialRoad route = routeOpt.get();
        if (NpcEntityIndex.innCustomers(level, request.innName()).size() >= Math.max(1, route.getPositions().size())) {
            return SpawnResult.FULL;
        }

        if (!route.getPositions().isEmpty() && route.getPositions().stream().noneMatch(level::hasChunkAt))
            return SpawnResult.NO_LOADED_POINT;
        BlockPos pos = randomFreePosition(level, route);
        if (pos == null) return SpawnResult.NO_FREE_POINT;

        String npcName = GlobalNpcManager.getRandomInactiveNpc();
        if (npcName == null) return SpawnResult.NO_NPC_AVAILABLE;

        Map<String, Object> sourceData = GlobalNpcManager.getNpcData(npcName);
        Map<String, Object> npcData = new HashMap<>(sourceData == null ? Map.of() : sourceData);
        npcData.put("Texts", java.util.List.of(
                "Bonjour, je voudrais " + request.itemDisplayName() + ".",
                "Est-ce que l'auberge sert encore " + request.itemDisplayName() + " ?"
        ));

        TradeNpc model = new TradeNpc(npcName, npcData, "inn_customer", pos);
        model.setRouteNpc(true);

        TradeNpcEntity entity = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), level);
        model.setNpcId(entity.getStringUUID());
        entity.setTradeNpc(model);
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setPersistenceRequired();

        CompoundTag data = entity.getPersistentData();
        data.putBoolean(INN_CUSTOMER_TAG, true);
        data.putString(INN_NAME_TAG, request.innName());
        data.putString(REQUEST_ITEM_ID_TAG, request.itemId());
        data.putString(REQUEST_ITEM_NAME_TAG, request.itemDisplayName());
        data.putLong(REQUEST_PRICE_TAG, request.price());
        data.putString(REQUEST_TYPE_TAG, request.type());

        if (!level.addFreshEntity(entity)) return SpawnResult.NO_FREE_POINT;
        GlobalNpcManager.activateNpc(model);
        return SpawnResult.SPAWNED;
    }

    public static boolean handleInnCustomerInteraction(ServerPlayer player, Entity entity) {
        if (player == null || entity == null || !entity.getPersistentData().getBoolean(INN_CUSTOMER_TAG)) {
            return false;
        }

        CompoundTag order = entity.getPersistentData();
        if (!player.isShiftKeyDown()) {
            player.sendSystemMessage(Component.literal("Commande : ")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(Component.literal(order.getString(REQUEST_ITEM_NAME_TAG) + " · "
                            + MedievalCoinsIntegration.formatMoney(order.getLong(REQUEST_PRICE_TAG)))
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));
            player.displayClientMessage(Component.literal("Accroupis-toi et fais un clic droit avec le plat en main.")
                    .withStyle(net.minecraft.ChatFormatting.GRAY), true);
            return true;
        }
        try {
            Class<?> service = Class.forName("fr.renblood.medievalstructures.manager.InnCustomerService");
            java.lang.reflect.Method method = service.getMethod("serveInnCustomer", ServerPlayer.class, Entity.class);
            Object result = method.invoke(null, player, entity);
            boolean handled = Boolean.TRUE.equals(result);
            if (handled && entity.getPersistentData().getBoolean(FINISHED_TAG) && entity instanceof TradeNpcEntity npc) {
                GlobalNpcManager.deactivateNpc(npc.getNpcName());
            }
            return handled;
        } catch (ReflectiveOperationException exception) {
            CompoundTag data = entity.getPersistentData();
            player.displayClientMessage(Component.literal(
                    "Demande auberge: " + data.getString(REQUEST_ITEM_NAME_TAG)
                            + " (" + MedievalCoinsIntegration.formatMoney(data.getLong(REQUEST_PRICE_TAG)) + ")."
            ), false);
            return true;
        }
    }

    public static void finishInnCustomer(Entity entity) {
        if (entity instanceof TradeNpcEntity npc && entity.getPersistentData().getBoolean(INN_CUSTOMER_TAG)) {
            GlobalNpcManager.deactivateNpc(npc.getNpcName());
        }
    }

    private static Optional<CommercialRoad> findRoute(String routeId) {
        return Npcshopkeeper.COMMERCIAL_ROADS.stream()
                .filter(road -> road.getId().equalsIgnoreCase(routeId) || road.getName().equalsIgnoreCase(routeId))
                .findFirst();
    }

    private static Optional<CommercialRoad> findInnMenuRoute(String routeId) {
        if (routeId == null || routeId.isBlank()) return Optional.empty();
        try {
            int index = Integer.parseInt(routeId);
            List<CommercialRoad> routes = innMenuRoutes();
            if (index >= 0 && index < routes.size()) return Optional.of(routes.get(index));
        } catch (NumberFormatException ignored) {
        }
        return findRoute(routeId).filter(CommercialRoad::isInnMenuRoute);
    }

    private static List<CommercialRoad> innMenuRoutes() {
        return Npcshopkeeper.COMMERCIAL_ROADS.stream().filter(CommercialRoad::isInnMenuRoute).toList();
    }

    private static BlockPos randomFreePosition(ServerLevel level, CommercialRoad route) {
        if (route.getPositions().isEmpty()) return null;
        var positions = new java.util.ArrayList<>(route.getPositions());
        java.util.Collections.shuffle(positions, RANDOM);
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos)) continue;
            AABB nearby = new AABB(pos).inflate(1.0D);
            boolean occupied = level.getEntitiesOfClass(TradeNpcEntity.class, nearby).stream()
                    .anyMatch(entity -> entity.getPersistentData().getBoolean(INN_CUSTOMER_TAG));
            if (!occupied) return pos;
        }
        return null;
    }

    public record InnCustomerRequest(String innName, String itemId, String itemDisplayName, long price, String type) {
    }

    public enum SpawnResult {
        SPAWNED,
        FULL,
        NO_ROUTE,
        NO_NPC_AVAILABLE,
        NO_FREE_POINT,
        NO_LOADED_POINT,
        INVALID
    }
}
