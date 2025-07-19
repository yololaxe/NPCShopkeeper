package fr.renblood.npcshopkeeper.data.io;

import com.google.gson.JsonObject;

public interface JsonSerializable<T> {
    /** Sérialise this en JsonObject. */
    JsonObject toJson();

    /** Désérialise depuis JsonObject. */
    static <T> T fromJson(JsonObject obj) {
        throw new UnsupportedOperationException("Implement in each model");
    }
}
