package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.api.NpcTemplate;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
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
import java.util.Locale;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateNpcButtonMessage {
    private final String name;
    private final String skin;
    private final String type; // DECO, SHOPKEEPER, QUEST
    private final List<String> texts;
    private final String tradeCategory;

    public CreateNpcButtonMessage(String name, String skin, String type, List<String> texts, String tradeCategory) {
        this.name = name;
        this.skin = skin;
        this.type = type;
        this.texts = texts;
        this.tradeCategory = tradeCategory;
    }

    public CreateNpcButtonMessage(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.skin = buffer.readUtf();
        this.type = buffer.readUtf();
        
        int size = buffer.readInt();
        this.texts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.texts.add(buffer.readUtf());
        }
        
        this.tradeCategory = buffer.readUtf();
    }

    public static void buffer(CreateNpcButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.name);
        buffer.writeUtf(message.skin);
        buffer.writeUtf(message.type);
        
        buffer.writeInt(message.texts.size());
        for (String text : message.texts) {
            buffer.writeUtf(text);
        }
        
        buffer.writeUtf(message.tradeCategory);
    }

    public static void handler(CreateNpcButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Création du template pour l'API
                NpcTemplate template = new NpcTemplate();
                
                // Génération d'un ID (slug) à partir du nom
                String slug = message.name.toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]+", "-")
                        .replaceAll("^-|-$", "");
                if (slug.isEmpty()) slug = "npc-" + System.currentTimeMillis();
                
                template.npc_id = slug;
                template.name = message.name;
                template.type = message.type;
                template.texture = message.skin;
                template.dialogue = message.texts;
                template.enabled = true;
                template.trade_category = message.tradeCategory;
                
                // Appel à l'API MedievalCoins
                boolean success = MedievalCoinsIntegration.createNpc(template);
                
                if (success) {
                    // Mise à jour immédiate de la liste locale pour que le PNJ soit disponible
                    GlobalNpcManager.addNpcFromTemplate(template);
                    
                    // Synchronisation avec le client (pour les suggestions)
                    PacketHandler.sendToPlayer(new SyncGlobalNpcDataPacket(GlobalNpcManager.getAllNpcData()), player);
                    
                    player.displayClientMessage(Component.translatable("command.npcshopkeeper.create_npc.success", message.name), false);
                } else {
                    // Fallback local si l'API échoue ou n'est pas dispo (pour compatibilité)
                    boolean isShopkeeper = "SHOPKEEPER".equals(message.type);
                    success = GlobalNpcManager.registerNewNpc(message.name, message.skin, isShopkeeper, message.texts);
                    
                    if (success) {
                        player.displayClientMessage(Component.translatable("command.npcshopkeeper.create_npc.success", message.name), false);
                    } else {
                        player.displayClientMessage(Component.translatable("command.npcshopkeeper.create_npc.failure"), false);
                    }
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
