package fr.renblood.npcshopkeeper.data.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class TradeList {
    private List<Trade> trades;

    // Constructeurs
    public TradeList() {}

    public TradeList(List<Trade> trades) {
        this.trades = trades;
    }

    // Getters et setters
    public List<Trade> getTrades() {
        return trades;
    }

    public void setTrades(List<Trade> trades) {
        this.trades = trades;
    }

    // Redéfinir toString pour faciliter le débogage
    @Override
    public String toString() {
        return "TradeList{" +
                "trades=" + trades +
                '}';
    }
    // Méthode pour récupérer les noms de tous les trades
    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (Trade trade : trades) {
            names.add(trade.getName());
        }
        return names;
    }

    // Méthode pour vérifier si un trade avec un nom donné existe
    public boolean existName(String name) {
        for (Trade trade : trades) {
            if (trade.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (Trade t : trades) {
            arr.add(t.toJson());
        }
        o.add("trades", arr);
        return o;
    }

    public static TradeList fromJson(JsonObject o) {
        List<Trade> items = new ArrayList<>();
        if (o.has("trades")) {
            for (JsonElement e : o.getAsJsonArray("trades")) {
                items.add(Trade.fromJson(e.getAsJsonObject()));
            }
        }
        return new TradeList(items);
    }
}
