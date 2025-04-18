package fr.renblood.npcshopkeeper.data.trade;

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
}
