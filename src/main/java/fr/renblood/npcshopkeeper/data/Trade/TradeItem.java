package fr.renblood.npcshopkeeper.data.Trade;

public class TradeItem {
    private String item;
    private int min;
    private int max;

    // Constructeurs
    public TradeItem() {}

    public TradeItem(String item, int min, int max) {
        this.item = item;
        this.min = min;
        this.max = max;
    }

    // Getters et setters
    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    // Redéfinir toString pour faciliter le débogage
    @Override
    public String toString() {
        return "TradeItem{" +
                "item='" + item + '\'' +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
