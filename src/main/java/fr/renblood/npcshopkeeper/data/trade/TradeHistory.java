package fr.renblood.npcshopkeeper.data.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.renblood.npcshopkeeper.data.price.TradeItemInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TradeHistory {
    private List<String> player;
    private String tradeName;
    private boolean isFinished;
    private boolean isAbandoned; // Nouveau champ
    private String id;
    private List<TradeItemInfo> tradeItems;
    private int totalPrice;
    private String npcId;
    private String npcName;


    // Constructeur principal
    public TradeHistory(List<String> player, String tradeName, boolean isFinished, String ID, List<TradeItemInfo> tradeItems, int totalPrice, String npcId, String npcName) {
        this(player, tradeName, isFinished, false, ID, tradeItems, totalPrice, npcId, npcName);
    }

    // Constructeur complet avec isAbandoned
    public TradeHistory(List<String> player, String tradeName, boolean isFinished, boolean isAbandoned, String ID, List<TradeItemInfo> tradeItems, int totalPrice, String npcId, String npcName) {
        this.player = player;
        this.tradeName = tradeName;
        this.isFinished = isFinished;
        this.isAbandoned = isAbandoned;
        this.id = ID;
        this.tradeItems = tradeItems;
        this.totalPrice = totalPrice;
        this.npcId = npcId;
        this.npcName = npcName;
    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getPlayer() {
        return player;
    }

    public void setPlayer(List<String> player) {
        this.player = player;
    }
    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public boolean isAbandoned() {
        return isAbandoned;
    }

    public void setAbandoned(boolean abandoned) {
        isAbandoned = abandoned;
    }

    public List<TradeItemInfo> getTradeItems() {
        return tradeItems;
    }

    public void setTradeItems(List<TradeItemInfo> tradeItems) {
        this.tradeItems = tradeItems;
    }
    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getNpcId() {
        return npcId;
    }

    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    @Override
    public String toString() {
        return "TradeHistory{" +
                "player='" + player + '\'' +
                ", tradeName='" + tradeName + '\'' +
                ", isFinished=" + isFinished +
                ", isAbandoned=" + isAbandoned +
                ", totalPrice=" + totalPrice +
                ", tradeItems=" + tradeItems +
                '}';
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();

        JsonArray pArr = new JsonArray();
        for (String p : player) {
            pArr.add(p);
        }
        o.add("players", pArr);

        o.addProperty("tradeName",  tradeName);
        o.addProperty("isFinished", isFinished);
        o.addProperty("isAbandoned", isAbandoned); // Sauvegarde du nouveau champ
        o.addProperty("id",         id);

        JsonArray itemsArr = new JsonArray();
        for (TradeItemInfo ti : tradeItems) {
            itemsArr.add(ti.toJson());
        }
        o.add("tradeItems", itemsArr);

        o.addProperty("totalPrice", totalPrice);
        o.addProperty("npcId",      npcId);
        o.addProperty("npcName",    npcName);

        return o;
    }

    public static TradeHistory fromJson(JsonObject o) {
        List<String> players = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("players")) {
            players.add(e.getAsString());
        }

        String tradeName  = o.get("tradeName").getAsString();
        boolean isFinished= o.get("isFinished").getAsBoolean();
        
        // Lecture du nouveau champ avec valeur par défaut false pour la rétrocompatibilité
        boolean isAbandoned = o.has("isAbandoned") && o.get("isAbandoned").getAsBoolean();
        
        String id         = o.get("id").getAsString();

        List<TradeItemInfo> items = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("tradeItems")) {
            items.add(TradeItemInfo.fromJson(e.getAsJsonObject()));
        }

        int totalPrice   = o.get("totalPrice").getAsInt();
        String npcId     = o.get("npcId").getAsString();
        String npcName   = o.get("npcName").getAsString();

        return new TradeHistory(players,
                tradeName,
                isFinished,
                isAbandoned,
                id,
                items,
                totalPrice,
                npcId,
                npcName);
    }
}
