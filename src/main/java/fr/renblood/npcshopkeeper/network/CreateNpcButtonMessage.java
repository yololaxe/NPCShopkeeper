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

import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateNpcButtonMessage {
    private final String name;
    private final String skin;
    private final boolean isShopkeeper;

    public CreateNpcButtonMessage(String name, String skin, boolean isShopkeeper) {
        this.name = name;
        this.skin = skin;
        this.isShopkeeper = isShopkeeper;
    }

    public CreateNpcButtonMessage(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.skin = buffer.readUtf();
        this.isShopkeeper = buffer.readBoolean();
    }

    public static void buffer(CreateNpcButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.name);
        buffer.writeUtf(message.skin);
        buffer.writeBoolean(message.isShopkeeper);
    }

    public static void handler(CreateNpcButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Traitement de la texture : si c'est un pseudo, on construit l'URL ou le chemin local
                // Pour simplifier ici, on suppose que si ça ne commence pas par "http", c'est un pseudo
                // et on utilise une texture locale ou une API de skin.
                // Dans votre mod actuel, vous utilisez des textures locales "textures/entity/nom.png".
                // On va garder cette logique pour l'instant ou adapter selon votre besoin.
                
                // Si l'utilisateur entre un pseudo, on peut imaginer un système qui télécharge le skin,
                // mais pour l'instant on va stocker la valeur brute.
                // Le rendu devra gérer ça.
                
                // Pour l'instant, on stocke ce que l'utilisateur a entré.
                String texturePath = message.skin;
                if (!texturePath.contains("/") && !texturePath.contains(":")) {
                     // Si c'est juste un nom (ex: "Notch"), on assume que c'est un fichier local standardisé
                     // ou on pourrait utiliser une URL de skin head
                     texturePath = "textures/entity/" + message.skin.toLowerCase() + ".png";
                }

                boolean success = GlobalNpcManager.registerNewNpc(message.name, texturePath, message.isShopkeeper);
                
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
