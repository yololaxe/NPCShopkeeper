package fr.renblood.npcshopkeeper.procedures.trade;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager; // for path constants
import fr.renblood.npcshopkeeper.data.price.TradeItemInfo;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Supplier;

public class TradePrepareProcedure {
    public static void execute(Entity entity,
                               String cleanTradeName,
                               String npcId,
                               String npcName) {
        if (!(entity instanceof ServerPlayer _player)) return;
        if (_player.containerMenu == null) return;

        // 1️⃣ Load all trades and find the one we want
        JsonRepository<Trade> tradeRepo = new JsonRepository<>(
                Paths.get(JsonFileManager.path),
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
            return;
        }

        // 2️⃣ Load history and check for an existing open trade for this NPC
        JsonRepository<TradeHistory> historyRepo = new JsonRepository<>(
                Paths.get(JsonFileManager.pathHistory),
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

        // 3️⃣ Prepare slots
        Object menu = _player.containerMenu;
        if (!(menu instanceof Supplier<?> sup && sup.get() instanceof Map<?, ?> _slots)) {
            _player.displayClientMessage(
                    Component.literal("containerMenu invalide pour le trade"),
                    false
            );
            return;
        }
        @SuppressWarnings("unchecked")
        Map<Integer, Slot> slots = (Map<Integer, Slot>) sup.get();

        Random rnd = new Random();
        List<TradeItemInfo> tradeItems = new ArrayList<>();
        // 4️⃣ Fill item slots
        int slotIndex = 0;
        for (var tItem : trade.getTrades()) {
            final int price, qty;
            List<TradeItemInfo> existingItems = checkExist
                    ? tradeHistory.getTradeItems()
                    : Collections.emptyList();

            if (checkExist) {
                // reuse existing
                TradeItemInfo info = existingItems.get(slotIndex / 2);
                price = info.getPrice();
                qty   = info.getQuantity();
            } else {
                // compute fresh
                Pair<Integer,Integer> minMax = PriceReferenceManager.findReferenceByItem(
                        tItem.getItem(), _player
                );
                price = new Random()
                        .nextInt(minMax.second - minMax.first + 1)
                        + minMax.first;
                qty   = tItem.getMin() +
                        new Random().nextInt(tItem.getMax() - tItem.getMin() + 1);
            }

            // now we know price & qty for sure:
            ItemStack stack = new ItemStack(
                    BuiltInRegistries.ITEM.get(new ResourceLocation(tItem.getItem())),
                    qty
            );
            slots.get(slotIndex).set(stack);

            // add a typed TradeItemInfo
            tradeItems.add(new TradeItemInfo(
                    tItem.getItem(),
                    qty,
                    price
            ));

            slotIndex += 2;
        }


        // 5️⃣ Set the “category” slot (12)
        ItemStack catStack = new ItemStack(
                BuiltInRegistries.ITEM.get(new ResourceLocation(trade.getCategory()))
        );
        catStack.setHoverName(Component.literal(trade.getName() + " " + id));
        slots.get(12).set(catStack);

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
        Item[] coinItems = {
                BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:gold_coin")),
                BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:silver_coin")),
                BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:bronze_coin")),
                Items.COPPER_INGOT
        };
        int coinSlot = 13;
        for (int i = 0; i < coins.length && coinSlot <= 14; i++) {
            if (coins[i] > 0) {
                slots.get(coinSlot).set(new ItemStack(coinItems[i], coins[i]));
                coinSlot++;
            }
        }

        // 7️⃣ Log the start of a new trade if needed
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
    }
}
