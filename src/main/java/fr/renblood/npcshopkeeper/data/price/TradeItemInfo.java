package fr.renblood.npcshopkeeper.data.price;

import net.minecraft.world.item.Item;

public class TradeItemInfo {
    private Item item;
    private int quantity;
    private int price;

    public TradeItemInfo(Item item, int quantity, int price) {
        this.item = item;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters
    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return "TradeItemInfo{" +
                "item=" + item +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
