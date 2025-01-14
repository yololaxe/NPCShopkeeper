package fr.renblood.npcshopkeeper.data.npc;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.Trade.Trade;
import fr.renblood.npcshopkeeper.data.Trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class TradeNpc {
    private String npcId;
    private String npcName;
    private Map<String, Object> npcData;
    private ArrayList<String> texts;
    private String texture;
    private String tradeCategory;
    private Trade trade;
    private TradeHistory tradeHistory;
    private BlockPos pos;

    private static final Logger LOGGER = LogManager.getLogger(TradeNpc.class);

    public TradeNpc(String npcName, Map<String, Object> npcData, String tradeCategory, BlockPos pos) {
        this.npcName = npcName != null ? npcName : "PNJ sans nom"; // Valeur par défaut
        this.npcData = npcData != null ? npcData : new HashMap<>(); // Prévenir null
        this.texts = (ArrayList<String>) this.npcData.getOrDefault("Texts", new ArrayList<>());
        this.texture = (String) this.npcData.getOrDefault("Texture", "textures/entity/"+npcName.toLowerCase()+".png");
        this.tradeCategory = tradeCategory != null ? tradeCategory : "null";
        this.pos = pos != null ? pos : new BlockPos(0,0,0);
    }
    public TradeNpc(String npcId, String npcName, Map<String, Object> npcData, String tradeCategory, BlockPos pos) {
        this.npcId = npcId;
        this.npcName = npcName != null ? npcName : "PNJ sans nom"; // Valeur par défaut
        this.npcData = npcData != null ? npcData : new HashMap<>(); // Prévenir null
        this.texts = (ArrayList<String>) this.npcData.getOrDefault("Texts", new ArrayList<>());
        this.texture = (String) this.npcData.getOrDefault("Texture", "textures/entity/banker.png");
        this.tradeCategory = tradeCategory != null ? tradeCategory : "null";
        this.pos = pos != null ? pos : new BlockPos(0,0,0);
    }


    public String getNpcId() {
        return npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public ArrayList<String> getTexts() {
        return texts;
    }

    public String getTexture() {
        return texture;
    }

    public String getTradeCategory() {
        return tradeCategory;
    }

    public void setTradeCategory(String tradeCategory) {
        this.tradeCategory = tradeCategory;
    }

    public Trade getTrade() {
        return trade;
    }

    public TradeHistory getTradeHistory() {
        return tradeHistory;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public void setTradeHistory(TradeHistory tradeHistory) {
        this.tradeHistory = tradeHistory;
    }

    public boolean hasTexts() {
        return texts != null && !texts.isEmpty();
    }

    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    public Map<String, Object> getNpcData() {
        return npcData;
    }

    public void setNpcData(Map<String, Object> npcData) {
        this.npcData = npcData;
    }

    public void setTexts(ArrayList<String> texts) {
        this.texts = texts;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public void interact(ServerPlayer player, BlockPos position) {

        if (hasTexts()) {
            String randomText = texts.get(new Random().nextInt(texts.size()));
            player.displayClientMessage(Component.literal(randomText), false);
        } else {
            player.displayClientMessage(Component.literal("Ce PNJ n'a rien à dire pour le moment."), false);
        }

        Pair<Boolean, TradeHistory> tradeStatus = JsonTradeFileManager.checkTradeStatusForNpc(npcId);
        if (tradeStatus.first) {
            tradeHistory = tradeStatus.second;
            if (tradeHistory == null || tradeHistory.getTradeName() == null || tradeHistory.getTradeItems().isEmpty()) {
                player.displayClientMessage(Component.literal("Erreur : données de trade manquantes pour le PNJ."), true);
                LOGGER.error("Données de trade manquantes pour le PNJ : " + npcId);
                return;
            }
            player.displayClientMessage(Component.literal("Un trade existant a été trouvé : " + tradeHistory.getTradeName()), true);
        } else if (tradeCategory != null) {
            List<Trade> trades = JsonTradeFileManager.getTradesByCategory(tradeCategory);
            if (!trades.isEmpty()) {
                trade = trades.get(new Random().nextInt(trades.size()));
                player.displayClientMessage(Component.literal("Nouveau trade généré : " + trade.getName()), true);
            } else {
                player.displayClientMessage(Component.literal("Aucun trade trouvé dans cette catégorie : " + tradeCategory), true);
            }
        } else {
            player.displayClientMessage(Component.literal("Ce PNJ n'a pas de catégorie de trade assignée."), true);
        }
    }
}
