package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Npcshopkeeper.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init() {
        int id = 0;
        INSTANCE.registerMessage(id++, TravelPacket.class, TravelPacket::toBytes, TravelPacket::new, TravelPacket::handle);
        INSTANCE.registerMessage(id++, SyncPortsPacket.class, SyncPortsPacket::toBytes, SyncPortsPacket::new, SyncPortsPacket::handle);
        INSTANCE.registerMessage(id++, SyncGlobalNpcDataPacket.class, SyncGlobalNpcDataPacket::toBytes, SyncGlobalNpcDataPacket::new, SyncGlobalNpcDataPacket::handle);
    }

    public static void sendToServer(Object msg) {
        INSTANCE.sendToServer(msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }
}
