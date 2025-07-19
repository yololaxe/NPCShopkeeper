package fr.renblood.npcshopkeeper.data.npc;

import com.google.gson.JsonObject;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import net.minecraft.core.BlockPos;
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
        this.texture = (String) this.npcData.getOrDefault("Texture", "textures/entity/" + npcName.toLowerCase() + ".png");
        this.tradeCategory = tradeCategory != null ? tradeCategory : "null";
        this.pos = pos != null ? pos : new BlockPos(0, 0, 0);

        LOGGER.info("PNJ créé sans ID : " + this.npcName + " | Texture : " + this.texture);
    }

    public TradeNpc(String npcId, String npcName, Map<String, Object> npcData, String tradeCategory, BlockPos pos) {
        this.npcId = npcId;
        this.npcName = npcName != null ? npcName : "PNJ sans nom"; // Valeur par défaut
        this.npcData = npcData != null ? npcData : new HashMap<>(); // Prévenir null
        this.texts = (ArrayList<String>) this.npcData.getOrDefault("Texts", new ArrayList<>());
        this.texture = (String) this.npcData.getOrDefault("Texture", "textures/entity/" + npcName.toLowerCase() + ".png");
        this.tradeCategory = tradeCategory != null ? tradeCategory : "null";
        this.pos = pos != null ? pos : new BlockPos(0, 0, 0);

        LOGGER.info("PNJ créé avec ID : " + this.npcId + " (" + this.npcName + ") | Texture : " + this.texture);
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

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id",        npcId);
        o.addProperty("name",      npcName);
        o.addProperty("texture",   texture);
        o.addProperty("category",  tradeCategory);
        o.addProperty("x",         pos.getX());
        o.addProperty("y",         pos.getY());
        o.addProperty("z",         pos.getZ());
        return o;
    }

    /**
     * Désérialise un TradeNpc depuis le JsonObject.
     * Utilise GlobalNpcManager pour récupérer npcData si besoin.
     */
    public static TradeNpc fromJson(JsonObject o) {
        String id       = o.get("id").getAsString();
        String name     = o.get("name").getAsString();
        String texture  = o.has("texture")  ? o.get("texture").getAsString()  : null;
        String category = o.has("category") ? o.get("category").getAsString() : null;
        int x = o.get("x").getAsInt();
        int y = o.get("y").getAsInt();
        int z = o.get("z").getAsInt();

        Map<String, Object> npcData =
                GlobalNpcManager.getNpcData(name);
        TradeNpc npc = new TradeNpc(id, name, npcData,
                category,
                new BlockPos(x, y, z));
        if (texture != null) {
            npc.setTexture(texture);
        }
        return npc;
    }

}
