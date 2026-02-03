package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.client.gui.TravelHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDepartureTimePacket {
    private final long departureTime;

    public SyncDepartureTimePacket(long departureTime) {
        this.departureTime = departureTime;
    }

    public SyncDepartureTimePacket(FriendlyByteBuf buf) {
        this.departureTime = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(departureTime);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client : mettre à jour le HUD
            TravelHudOverlay.setDepartureTime(departureTime);
        });
        ctx.get().setPacketHandled(true);
    }
}
