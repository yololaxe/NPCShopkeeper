package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncHarborConfigPacket {
    private final int blocksPerIron;
    private final int dayLengthInMinutes;

    public SyncHarborConfigPacket(int blocksPerIron, int dayLengthInMinutes) {
        this.blocksPerIron = blocksPerIron;
        this.dayLengthInMinutes = dayLengthInMinutes;
    }

    public SyncHarborConfigPacket(FriendlyByteBuf buf) {
        this.blocksPerIron = buf.readInt();
        this.dayLengthInMinutes = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(blocksPerIron);
        buf.writeInt(dayLengthInMinutes);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client : mettre à jour la config locale
            PortManager.setClientConfig(blocksPerIron, dayLengthInMinutes);
        });
        ctx.get().setPacketHandled(true);
    }
}
