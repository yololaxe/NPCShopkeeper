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
        if (!(_player.containerMenu instanceof fr.renblood.npcshopkeeper.world.inventory.TradeMenu expectedTrade)
                || !Objects.equals(expectedTrade.tradeName, cleanTradeName) || !Objects.equals(expectedTrade.npcId, npcId)) return;

        // LOG DE DEBUG CRITIQUE
        Npcshopkeeper.debugLog(LOGGER, ">>> TradePrepareProcedure START <<<");
        Npcshopkeeper.debugLog(LOGGER, "Trade: " + cleanTradeName + ", NPC: " + npcName + " (" + npcId + ")");

        // 1️⃣ Load all trades and find the one we want
        Trade trade = fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache.getTrade(cleanTradeName);
        if (trade == null) {
            _player.displayClientMessage(
                    Component.literal("Trade inconnu : " + cleanTradeName),
                    false
            );
            LOGGER.error("Trade not found: " + cleanTradeName);
            return;
        }

        if(trade.getTrades()==null||trade.getTrades().isEmpty()||trade.getTrades().size()>4){
            _player.sendSystemMessage(Component.literal("Cette offre doit contenir entre 1 et 4 lots."));return;
        }
        for(var requested:trade.getTrades()){
            var item=BuiltInRegistries.ITEM.get(new ResourceLocation(requested.getItem()));
            if(requested.getMin()<1||requested.getMax()<requested.getMin()||requested.getMax()>item.getMaxStackSize()){
                _player.sendSystemMessage(Component.literal("Offre à corriger : chaque lot doit tenir dans un emplacement."));return;
            }
        }
        fr.renblood.npcshopkeeper.manager.integration.CommerceAdapter.status(_player,"npc-contracts");
        // 2️⃣ Load history and check for an existing open trade for this NPC
        JsonRepository<TradeHistory> historyRepo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_HISTORY),
                "history",
                TradeHistory::fromJson,
                TradeHistory::toJson
        );
        Optional<TradeHistory> ongoing = fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache.getOpenHistory(npcId);

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
            final long price;
            final int qty;

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
                Pair<Long,Long> minMax = PriceReferenceManager.findReferenceByItem(
                        tItem.getItem(), _player
                );
                if(minMax.getLeft()<1||minMax.getRight()<minMax.getLeft()){
                    _player.sendSystemMessage(Component.literal("Prix inconnu pour "+tItem.getItem()+". Offre suspendue."));return;
                }
                price = rnd.nextLong(minMax.getRight() - minMax.getLeft() + 1) + minMax.getLeft();
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

        long totalCopper;
        try{totalCopper=MoneyCalculator.calculateTotalMoneyFromTrade(flat);}
        catch(IllegalArgumentException|ArithmeticException invalid){_player.sendSystemMessage(Component.literal("Montant de l’offre invalide. Aucun paiement lancé."));return;}
        ItemStack bankReceipt=new ItemStack(Items.PAPER);
        bankReceipt.setHoverName(Component.literal("Paiement en pièces : "
                + fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration.formatMoney(totalCopper))
                .withStyle(net.minecraft.ChatFormatting.GOLD));
        if(slots.containsKey(13))slots.get(13).set(bankReceipt);
        if(slots.containsKey(14))slots.get(14).set(ItemStack.EMPTY);

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
            try { historyRepo.add(newHistory); }
            catch (java.io.UncheckedIOException failure) {
                TradeSessionManager.clearTradeId(_player.getUUID());
                _player.displayClientMessage(Component.literal("Contrat non préparé : la sauvegarde locale doit être vérifiée par l’équipe.")
                        .withStyle(net.minecraft.ChatFormatting.RED), false);
                if (_player.containerMenu == expectedTrade) _player.closeContainer();
                return;
            }
        }
        
        Npcshopkeeper.debugLog(LOGGER, ">>> TradePrepareProcedure END <<<");
    }
}
