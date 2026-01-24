package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncPortsPacket {
    private final List<Port> ports;

    public SyncPortsPacket(List<Port> ports) {
        this.ports = ports;
    }

    public SyncPortsPacket(FriendlyByteBuf buf) {
        this.ports = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            // Lecture simplifiée : nom, x, y, z, dimension
            String name = buf.readUtf();
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            String dim = buf.readUtf();
            this.ports.add(new Port(name, new net.minecraft.core.BlockPos(x, y, z), dim));
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(ports.size());
        for (Port p : ports) {
            buf.writeUtf(p.getName());
            buf.writeInt(p.getPos().getX());
            buf.writeInt(p.getPos().getY());
            buf.writeInt(p.getPos().getZ());
            buf.writeUtf(p.getDimension());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Côté client : mettre à jour la liste des ports
            PortManager.setClientPorts(this.ports);
        });
        ctx.get().setPacketHandled(true);
    }
}
