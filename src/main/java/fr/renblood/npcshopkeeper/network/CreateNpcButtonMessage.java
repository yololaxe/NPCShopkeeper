package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateNpcButtonMessage {
    private final String name;
    private final String skin;
    private final boolean isShopkeeper;
    private final List<String> texts;

    public CreateNpcButtonMessage(String name, String skin, boolean isShopkeeper, List<String> texts) {
        this.name = name;
        this.skin = skin;
        this.isShopkeeper = isShopkeeper;
        this.texts = texts;
    }

    public CreateNpcButtonMessage(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.skin = buffer.readUtf();
        this.isShopkeeper = buffer.readBoolean();
        
        int size = buffer.readInt();
        this.texts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.texts.add(buffer.readUtf());
        }
    }

    public static void buffer(CreateNpcButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.name);
        buffer.writeUtf(message.skin);
        buffer.writeBoolean(message.isShopkeeper);
        
        buffer.writeInt(message.texts.size());
        for (String text : message.texts) {
            buffer.writeUtf(text);
        }
    }

    public static void handler(CreateNpcButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                String texturePath = message.skin;

                boolean success = GlobalNpcManager.registerNewNpc(message.name, texturePath, message.isShopkeeper, message.texts);
                
                if (success) {
                    player.displayClientMessage(Component.literal("PNJ créé avec succès : " + message.name), false);
                } else {
                    player.displayClientMessage(Component.literal("Erreur : Ce nom de PNJ existe déjà !"), false);
                }
            }
        });
        context.setPacketHandled(true);
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        Npcshopkeeper.addNetworkMessage(CreateNpcButtonMessage.class, CreateNpcButtonMessage::buffer, CreateNpcButtonMessage::new, CreateNpcButtonMessage::handler);
    }
}
