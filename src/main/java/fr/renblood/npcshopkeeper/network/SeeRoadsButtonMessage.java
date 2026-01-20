package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.command.SeeRoadsCommand;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.world.inventory.RoadDetailsMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SeeRoadsButtonMessage {
    private final int type; // 0 = page, 1 = open road details
    private final int page;
    private final String roadId;

    public SeeRoadsButtonMessage(int page) {
        this.type = 0;
        this.page = page;
        this.roadId = "";
    }

    public SeeRoadsButtonMessage(String roadId) {
        this.type = 1;
        this.page = 0;
        this.roadId = roadId;
    }

    public SeeRoadsButtonMessage(FriendlyByteBuf buffer) {
        this.type = buffer.readInt();
        this.page = buffer.readInt();
        this.roadId = buffer.readUtf();
    }

    public static void buffer(SeeRoadsButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.type);
        buffer.writeInt(message.page);
        buffer.writeUtf(message.roadId);
    }

    public static void handler(SeeRoadsButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (message.type == 0) {
                    SeeRoadsCommand.openGui(player, message.page);
                } else if (message.type == 1) {
                    openRoadDetails(player, message.roadId);
                }
            }
        });
        context.setPacketHandled(true);
    }

    private static void openRoadDetails(ServerPlayer player, String roadId) {
        CommercialRoad road = Npcshopkeeper.COMMERCIAL_ROADS.stream()
                .filter(r -> r.getId().equals(roadId))
                .findFirst()
                .orElse(null);

        if (road != null) {
            NetworkHooks.openScreen(player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Détails Route: " + road.getName());
                }

                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeUtf(roadId);
                    return new RoadDetailsMenu(id, inventory, buf);
                }
            }, buf -> buf.writeUtf(roadId));
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        Npcshopkeeper.addNetworkMessage(SeeRoadsButtonMessage.class, SeeRoadsButtonMessage::buffer, SeeRoadsButtonMessage::new, SeeRoadsButtonMessage::handler);
    }
}
