package fr.renblood.npcshopkeeper.data;

import java.util.List;

public class Trade {
    private String name;
    private String category;
    private List<TradeItem> trades;
    private TradeResult result;

    // Constructeurs
    public Trade() {}

    public Trade(String name, String category, List<TradeItem> trades, TradeResult result) {
        this.name = name;
        this.category = category;
        this.trades = trades;
        this.result = result;
    }

    // Getters et setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<TradeItem> getTrades() {
        return trades;
    }

    public void setTrades(List<TradeItem> trades) {
        this.trades = trades;
    }

    public TradeResult getResult() {
        return result;
    }

    public void setResult(TradeResult result) {
        this.result = result;
    }

    // Redéfinir toString pour faciliter le débogage
    @Override
    public String toString() {
        return "Trade{" +
                "name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", trades=" + trades +
                ", result=" + result +
                '}';
    }
}
