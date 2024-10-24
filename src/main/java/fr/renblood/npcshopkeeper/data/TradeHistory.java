package fr.renblood.npcshopkeeper.data;

import java.util.List;
import java.util.Map;

public class TradeHistory {
    private String player;
    private String tradeName;
    private boolean isFinished;
    private String id;
    private List<Map<String, Object>> tradeItems;
    private int totalPrice;

    // Constructeur
    public TradeHistory(String player, String tradeName, boolean isFinished, String ID, List<Map<String, Object>> tradeItems, int totalPrice) {
        this.player = player;
        this.tradeName = tradeName;
        this.isFinished = isFinished;
        this.id = ID;
        this.tradeItems = tradeItems;
        this.totalPrice = totalPrice;

    }

    // Getters et Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
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
    public List<Map<String, Object>> getTradeItems() {
        return tradeItems;
    }

    public void setTradeItems(List<Map<String, Object>> tradeItems) {
        this.tradeItems = tradeItems;
    }
    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    @Override
    public String toString() {
        return "TradeHistory{" +
                "player='" + player + '\'' +
                ", tradeName='" + tradeName + '\'' +
                ", isFinished=" + isFinished +
                ", totalPrice=" + totalPrice +
                ", tradeItems=" + tradeItems +
                '}';
    }
}
