package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RoadDetailsButtonMessage {
    private final int buttonID;
    private final String roadId;

    public RoadDetailsButtonMessage(int buttonID, String roadId) {
        this.buttonID = buttonID;
        this.roadId = roadId;
    }

    public RoadDetailsButtonMessage(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();
        this.roadId = buffer.readUtf();
    }

    public static void buffer(RoadDetailsButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.buttonID);
        buffer.writeUtf(message.roadId);
    }

    public static void handler(RoadDetailsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            CommercialRoad road = Npcshopkeeper.COMMERCIAL_ROADS.stream()
                    .filter(r -> r.getId().equals(message.roadId))
                    .findFirst()
                    .orElse(null);

            if (road == null) return;

            if (message.buttonID == 0) { // Supprimer la route
                deleteRoad(player, road);
            } else if (message.buttonID == 1) { // Téléporter
                teleportToRoad(player, road);
            }
        });
        context.setPacketHandled(true);
    }

    private static void deleteRoad(ServerPlayer player, CommercialRoad road) {
        // 1. Supprimer les PNJs de la route
        for (TradeNpcEntity npc : road.getNpcEntities()) {
            npc.discard();
            GlobalNpcManager.deactivateNpc(npc.getNpcName());
            ActiveNpcManager.removeActiveNpc(npc.getUUID());
        }

        // 2. Supprimer la route de la liste en mémoire
        Npcshopkeeper.COMMERCIAL_ROADS.remove(road);

        // 3. Sauvegarder les changements dans le JSON
        JsonRepository<CommercialRoad> repo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, player.serverLevel()),
                CommercialRoad::toJson
        );
        repo.saveAll(Npcshopkeeper.COMMERCIAL_ROADS);

        player.displayClientMessage(Component.literal("Route supprimée : " + road.getName()), false);
    }

    private static void teleportToRoad(ServerPlayer player, CommercialRoad road) {
        BlockPos targetPos = null;

        // Essayer de se téléporter au premier PNJ
        if (!road.getNpcEntities().isEmpty()) {
            TradeNpcEntity npc = road.getNpcEntities().get(0);
            targetPos = npc.blockPosition();
        } 
        // Sinon, au premier point de la route
        else if (!road.getPositions().isEmpty()) {
            targetPos = road.getPositions().get(0);
        }

        if (targetPos != null) {
            player.teleportTo(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            player.displayClientMessage(Component.literal("Téléporté à la route : " + road.getName()), false);
        } else {
            player.displayClientMessage(Component.literal("Impossible de se téléporter : aucun point ou PNJ trouvé."), false);
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        Npcshopkeeper.addNetworkMessage(RoadDetailsButtonMessage.class, RoadDetailsButtonMessage::buffer, RoadDetailsButtonMessage::new, RoadDetailsButtonMessage::handler);
    }
}
