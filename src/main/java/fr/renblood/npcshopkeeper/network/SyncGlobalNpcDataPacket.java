package fr.renblood.npcshopkeeper.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class SyncGlobalNpcDataPacket {
    private final String jsonData;

    public SyncGlobalNpcDataPacket(Map<String, Map<String, Object>> data) {
        Gson gson = new Gson();
        this.jsonData = gson.toJson(data);
    }

    public SyncGlobalNpcDataPacket(FriendlyByteBuf buf) {
        this.jsonData = buf.readUtf(32767 * 4); // Grande taille pour être sûr
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.jsonData);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
            Map<String, Map<String, Object>> data = gson.fromJson(this.jsonData, type);
            GlobalNpcManager.setClientData(data);
        });
        ctx.get().setPacketHandled(true);
    }
}
