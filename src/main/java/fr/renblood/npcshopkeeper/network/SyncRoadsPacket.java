package fr.renblood.npcshopkeeper.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncRoadsPacket {
    private static final int MAX_JSON_LENGTH = 32767 * 8;
    private static volatile List<CommercialRoad> clientRoads = List.of();
    private final String jsonData;

    public SyncRoadsPacket(List<CommercialRoad> roads) {
        JsonArray summaries = new JsonArray();
        for (CommercialRoad road : roads) {
            var summary = road.toJson();
            summary.remove("npcEntities");
            summaries.add(summary);
        }
        this.jsonData = summaries.toString();
    }

    public SyncRoadsPacket(FriendlyByteBuf buf) {
        this.jsonData = buf.readUtf(MAX_JSON_LENGTH);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(jsonData, MAX_JSON_LENGTH);
    }

    public static List<CommercialRoad> clientRoads() {
        return clientRoads;
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> {
                try {
                    List<CommercialRoad> summaries = new ArrayList<>();
                    for (var entry : JsonParser.parseString(jsonData).getAsJsonArray()) {
                        var summary = entry.getAsJsonObject();
                        summary.remove("npcEntities");
                        summaries.add(CommercialRoad.fromJson(summary, null));
                    }
                    clientRoads = List.copyOf(summaries);
                } catch (RuntimeException failure) {
                    Npcshopkeeper.LOGGER.error("Impossible de lire la liste des routes", failure);
                }
            });
        }
        context.setPacketHandled(true);
    }
}
