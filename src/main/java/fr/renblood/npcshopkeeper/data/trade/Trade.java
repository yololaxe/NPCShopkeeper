package fr.renblood.npcshopkeeper.data.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
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
    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("Name", name);
        o.addProperty("Category", category);

        JsonArray arr = new JsonArray();
        for (TradeItem ti : trades) arr.add(ti.toJson());
        o.add("trades", arr);

        if (result != null) o.add("result", result.toJson());
        return o;
    }

    public static Trade fromJson(JsonObject o) {
        String name     = o.get("Name").getAsString();
        String category = o.get("Category").getAsString();

        List<TradeItem> items = new ArrayList<>();
        JsonArray arr = o.getAsJsonArray("trades");
        for (JsonElement e : arr) {
            if (e.isJsonObject()) {
                items.add(TradeItem.fromJson(e.getAsJsonObject()));
            }
        }

        TradeResult res = null;
        // only parse "result" if it's actually a JSON object
        if (o.has("result") && o.get("result").isJsonObject()) {
            res = TradeResult.fromJson(o.getAsJsonObject("result"));
        }

        return new Trade(name, category, items, res);
    }

}
