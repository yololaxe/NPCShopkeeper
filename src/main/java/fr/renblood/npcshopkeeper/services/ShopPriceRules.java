package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
/** Workshop rules independent of client screens and network requests. */
public final class ShopPriceRules {
    public static long quote(JsonObject data, JsonObject item) {
        if (data.has("references")) for (var entry : data.getAsJsonArray("references")) {
            var ref = entry.getAsJsonObject();
            if (ref.get("item_id").getAsString().equals(item.get("item_id").getAsString())) {
                try { return Math.max(0, ref.get("price").getAsLong()); } catch (Exception missing) { return 0; }
            }
        }
        return 0;
    }
    public static boolean automatic(JsonObject data, JsonObject item) {
        long price = quote(data, item);
        boolean wasAutomatic = item.has("automatic") && item.get("automatic").getAsBoolean();
        item.addProperty("automatic", price > 0);
        if (price > 0) {
            item.addProperty("buy_price", Long.toString(price));
            item.addProperty("sell_price", Long.toString(Math.max(1, price - price / 5 - (price % 5 == 0 ? 0 : 1))));
        } else if (wasAutomatic) {
            item.addProperty("buy_price", "0"); item.addProperty("sell_price", "0");
        }
        return price > 0;
    }
}
