package fr.renblood.npcshopkeeper.data.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class TradeHistoryList {
    private List<TradeHistory> tradeHistories;

    // Constructeur
    public TradeHistoryList() {
        this.tradeHistories = new ArrayList<>();
    }

    // Ajouter un TradeHistory à la liste
    public void addTradeHistory(TradeHistory tradeHistory) {
        this.tradeHistories.add(tradeHistory);
    }

    // Récupérer la liste des TradeHistory
    public List<TradeHistory> getTradeHistories() {
        return tradeHistories;
    }

    // Trouver un TradeHistory par nom de joueur et nom de trade
    public TradeHistory findTradeByPlayerAndName(String player, String tradeName) {
        for (TradeHistory tradeHistory : tradeHistories) {
            if (tradeHistory.getPlayer().equals(player) && tradeHistory.getTradeName().equals(tradeName)) {
                return tradeHistory;
            }
        }
        return null; // Si aucun TradeHistory n'est trouvé
    }

    @Override
    public String toString() {
        return "TradeHistoryList{" +
                "tradeHistories=" + tradeHistories +
                '}';
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        JsonArray arr = new JsonArray();
        for (TradeHistory th : tradeHistories) {
            arr.add(th.toJson());
        }
        o.add("tradeHistories", arr);
        return o;
    }

    public static TradeHistoryList fromJson(JsonObject o) {
        TradeHistoryList list = new TradeHistoryList();
        if (o.has("tradeHistories")) {
            for (JsonElement e : o.getAsJsonArray("tradeHistories")) {
                list.addTradeHistory(
                        TradeHistory.fromJson(e.getAsJsonObject())
                );
            }
        }
        return list;
    }
}
