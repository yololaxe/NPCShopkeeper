package fr.renblood.npcshopkeeper.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncRoadsPacket {
    private final String jsonData;

    public SyncRoadsPacket(List<CommercialRoad> roads) {
        Gson gson = new Gson();
        // On convertit la liste en JSON. Attention aux références circulaires si CommercialRoad en a (normalement non).
        // On peut créer une liste simplifiée si besoin, mais CommercialRoad semble être un POJO de données.
        this.jsonData = gson.toJson(roads);
    }

    public SyncRoadsPacket(FriendlyByteBuf buf) {
        this.jsonData = buf.readUtf(32767 * 8); // Grande taille pour la liste
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.jsonData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<CommercialRoad>>(){}.getType();
                List<CommercialRoad> roads = gson.fromJson(this.jsonData, type);
                
                // Mise à jour de la liste côté client
                Npcshopkeeper.COMMERCIAL_ROADS.clear();
                if (roads != null) {
                    Npcshopkeeper.COMMERCIAL_ROADS.addAll(roads);
                }
                Npcshopkeeper.LOGGER.info("Routes commerciales synchronisées côté client : " + Npcshopkeeper.COMMERCIAL_ROADS.size());
            } catch (Exception e) {
                Npcshopkeeper.LOGGER.error("Erreur lors de la désérialisation des routes côté client", e);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
