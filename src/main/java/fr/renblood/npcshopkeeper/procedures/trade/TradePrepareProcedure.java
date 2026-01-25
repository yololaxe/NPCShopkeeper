package fr.renblood.npcshopkeeper.procedures.trade;

import org.apache.commons.lang3.tuple.Pair;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.price.TradeItemInfo;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager;
import fr.renblood.npcshopkeeper.manager.trade.TradeSessionManager;
import fr.renblood.npcshopkeeper.manager.trade.XpReferenceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public class TradePrepareProcedure {
    private static final Logger LOGGER = LogManager.getLogger(TradePrepareProcedure.class);

    public static void execute(Entity entity,
                               String cleanTradeName,
                               String npcId,
                               String npcName) {
        if (!(entity instanceof ServerPlayer _player)) return;

        // LOG DE DEBUG CRITIQUE
        Npcshopkeeper.debugLog(LOGGER, ">>> TradePrepareProcedure START <<<");
        Npcshopkeeper.debugLog(LOGGER, "Trade: " + cleanTradeName + ", NPC: " + npcName + " (" + npcId + ")");

        // 1️⃣ Load all trades and find the one we want
        JsonRepository<Trade> tradeRepo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH),
                "trades",
                Trade::fromJson,
                Trade::toJson
        );
        Trade trade = tradeRepo.loadAll().stream()
                .filter(t -> t.getName().equals(cleanTradeName))
                .findFirst()
                .orElse(null);
        if (trade == null) {
            _player.displayClientMessage(
                    Component.literal("Trade inconnu : " + cleanTradeName),
                    false
            );
            LOGGER.error("Trade not found: " + cleanTradeName);
            return;
        }

        // 2️⃣ Load history and check for an existing open trade for this NPC
        JsonRepository<TradeHistory> historyRepo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_HISTORY),
                "history",
                TradeHistory::fromJson,
                TradeHistory::toJson
        );
        List<TradeHistory> allHistories = historyRepo.loadAll();
        Optional<TradeHistory> ongoing = allHistories.stream()
                .filter(h -> !h.isFinished() && h.getNpcId().equals(npcId))
                .findFirst();

        boolean checkExist = ongoing.isPresent();
        TradeHistory tradeHistory = ongoing.orElse(null);
        String id = checkExist
                ? tradeHistory.getId()
                : UUID.randomUUID().toString();
        
        if (checkExist && (id == null || id.isEmpty())) {
            LOGGER.warn("TradeHistory ID is null or empty! Generating new ID.");
            id = UUID.randomUUID().toString();
        }

        Npcshopkeeper.debugLog(LOGGER, "Trade ID: " + id + " (Existing: " + checkExist + ")");
        
        // SAUVEGARDE DE L'ID DANS LA SESSION (CRUCIAL POUR LA VALIDATION/ANNULATION)
        TradeSessionManager.setTradeId(_player.getUUID(), id);
        Npcshopkeeper.debugLog(LOGGER, "Trade ID saved to session for player " + _player.getName().getString());

        // 3️⃣ Prepare slots
        Object menu = _player.containerMenu;
        if (!(menu instanceof Supplier<?> sup && sup.get() instanceof Map<?, ?> rawSlots)) {
            _player.displayClientMessage(
                    Component.literal("containerMenu invalide pour le trade"),
                    false
            );
            LOGGER.error("Invalid containerMenu");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Integer, Slot> slots = (Map<Integer, Slot>) rawSlots;
        
        Npcshopkeeper.debugLog(LOGGER, "Container ID: " + _player.containerMenu.containerId);

        Random rnd = new Random();
        List<TradeItemInfo> tradeItems = new ArrayList<>();

        // 4️⃣ Fill item slots
        List<TradeItemInfo> existingItems = checkExist
                ? tradeHistory.getTradeItems()
                : Collections.emptyList();

        int slotIndex = 0;
        for (var tItem : trade.getTrades()) {
            final int price, qty;

            if (checkExist) {
                // reuse existing
                int historyIndex = slotIndex / 2;
                if (historyIndex < existingItems.size()) {
                    TradeItemInfo info = existingItems.get(historyIndex);
                    price = info.getPrice();
                    qty   = info.getQuantity();
                } else {
                    price = 0;
                    qty = 0;
                }
            } else {
                // compute fresh
                Pair<Integer,Integer> minMax = PriceReferenceManager.findReferenceByItem(
                        tItem.getItem(), _player
                );
                price = rnd.nextInt(minMax.getRight() - minMax.getLeft() + 1) + minMax.getLeft();
                qty   = tItem.getMin() + rnd.nextInt(tItem.getMax() - tItem.getMin() + 1);
            }

            // now we know price & qty for sure:
            ItemStack stack = new ItemStack(
                    BuiltInRegistries.ITEM.get(new ResourceLocation(tItem.getItem())),
                    qty
            );
            if (slots.containsKey(slotIndex)) {
                slots.get(slotIndex).set(stack);
            }

            // add a typed TradeItemInfo
            tradeItems.add(new TradeItemInfo(
                    tItem.getItem(),
                    qty,
                    price
            ));

            slotIndex += 2;
        }


        // 5️⃣ Set the “category” slot (12)
        // Gestion robuste de la catégorie : si l'item n'existe pas, on utilise un fallback
        Item categoryItem = Items.FILLED_MAP; // Fallback par défaut
        try {
            if (trade.getCategory() != null && !trade.getCategory().isEmpty()) {
                ResourceLocation res = new ResourceLocation(trade.getCategory());
                if (BuiltInRegistries.ITEM.containsKey(res)) {
                    categoryItem = BuiltInRegistries.ITEM.get(res);
                }
            }
        } catch (Exception e) {
            // Ignorer si la catégorie n'est pas un item valide
        }
        
        ItemStack catStack = new ItemStack(categoryItem);
        String fullName = trade.getName() + " " + id;
        catStack.setHoverName(Component.literal(fullName));
        
        Npcshopkeeper.debugLog(LOGGER, "Setting slot 12 with: " + fullName + " (Item: " + categoryItem + ")");
        
        if (slots.containsKey(12)) {
            slots.get(12).set(catStack);
            Npcshopkeeper.debugLog(LOGGER, "Slot 12 set successfully.");
        } else {
            LOGGER.error("Slot 12 not found in menu!");
        }

        // 6️⃣ Set coins
        List<Map<String,Object>> flat = tradeItems.stream().map(info -> {
            Map<String,Object> m = new HashMap<>();
            m.put("item",     info.getItem());
            m.put("quantity", info.getQuantity());
            m.put("price",    info.getPrice());
            return m;
        }).toList();

        int totalCopper = MoneyCalculator.calculateTotalMoneyFromTrade(flat);

        int[] coins = MoneyCalculator.getIntInCoins(totalCopper);
        String[] coinIds = {
                "medieval_coins:gold_coin",
                "medieval_coins:silver_coin",
                "medieval_coins:bronze_coin",
                "medieval_coins:iron_coin"
        };

        int coinSlot = 13;
        for (int i = 0; i < coins.length && coinSlot <= 14; i++) {
            if (coins[i] > 0) {
                Item coinItem = BuiltInRegistries.ITEM.get(new ResourceLocation(coinIds[i]));
                if (slots.containsKey(coinSlot)) {
                    slots.get(coinSlot).set(new ItemStack(coinItem, coins[i]));
                }
                coinSlot++;
            }
        }
        
        // 7️⃣ Set XP Info (Slot 15)
        if (slots.containsKey(15)) {
            float totalXp = 0;
            for (TradeItemInfo info : tradeItems) {
                XpReferenceManager.XpInfo xpRef = XpReferenceManager.getXpReference(info.getItem());
                if (xpRef != null) {
                    totalXp += xpRef.getAverageXp() * info.getQuantity();
                }
            }
            
            if (totalXp > 0) {
                ItemStack xpStack = new ItemStack(Items.GLOWSTONE_DUST);
                xpStack.setHoverName(Component.literal("XP Estimée: " + Math.round(totalXp)));
                slots.get(15).set(xpStack);
            } else {
                slots.get(15).set(ItemStack.EMPTY);
            }
        }
        
        // Force la mise à jour du container pour le client
        _player.containerMenu.broadcastChanges();
        Npcshopkeeper.debugLog(LOGGER, "Broadcast changes sent.");

        // 8️⃣ Log the start of a new trade if needed
        if (!checkExist) {
            _player.displayClientMessage(
                    Component.literal("Enregistrement du trade dans l'historique…"),
                    false
            );
            var newHistory = new TradeHistory(
                    List.of(_player.getGameProfile().getName()),
                    cleanTradeName,
                    false,
                    id,
                    tradeItems,
                    totalCopper,
                    npcId,
                    npcName
            );
            historyRepo.add(newHistory);
        }
        
        Npcshopkeeper.debugLog(LOGGER, ">>> TradePrepareProcedure END <<<");
    }
}
