package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.client.gui.TravelHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncDepartureTimePacket {
    private final long departureTime;
    private final long remainingSeconds;

    public SyncDepartureTimePacket(long departureTime, long remainingSeconds) {
        this.departureTime = departureTime;
        this.remainingSeconds = remainingSeconds;
    }

    public SyncDepartureTimePacket(FriendlyByteBuf buf) {
        this.departureTime = buf.readLong();
        this.remainingSeconds = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(departureTime);
        buf.writeLong(remainingSeconds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client : mettre à jour le HUD
            TravelHudOverlay.setDepartureTime(departureTime, remainingSeconds);
        });
        ctx.get().setPacketHandled(true);
    }
}
