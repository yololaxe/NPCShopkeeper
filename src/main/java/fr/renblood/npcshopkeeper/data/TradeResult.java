package fr.renblood.npcshopkeeper.data;

public class TradeResult {
    private String item;
    private int quantity;

    // Constructeurs
    public TradeResult() {}

    public TradeResult(String item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    // Getters et setters
    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // Redéfinir toString pour faciliter le débogage
    @Override
    public String toString() {
        return "TradeResult{" +
                "item='" + item + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
