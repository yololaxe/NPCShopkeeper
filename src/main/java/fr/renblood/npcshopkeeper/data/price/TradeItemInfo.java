package fr.renblood.npcshopkeeper.data.price;

import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;

public class TradeItemInfo {
    private String item;
    private int quantity;
    private int price;

    public TradeItemInfo(String item, int quantity, int price) {
        this.item = item;
        this.quantity = quantity;
        this.price = price;
    }

    // Getters
    public String getItem() {
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

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("item",     item);
        o.addProperty("quantity", quantity);
        o.addProperty("price",    price);
        return o;
    }

    public static TradeItemInfo fromJson(JsonObject o) {
        String item     = o.get("item").getAsString();
        int quantity    = o.get("quantity").getAsInt();
        int price       = o.get("price").getAsInt();
        return new TradeItemInfo(item, quantity, price);
    }
}
