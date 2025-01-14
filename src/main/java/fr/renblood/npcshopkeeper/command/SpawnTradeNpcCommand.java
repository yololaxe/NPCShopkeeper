package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.ActiveNpcManager;
import fr.renblood.npcshopkeeper.manager.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Map;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod.EventBusSubscriber
public class SpawnTradeNpcCommand {

    private static boolean isCommandRunning = false; // Ajout d'un verrou pour éviter les appels multiples

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("spawntradenpc")
                .requires(s -> s.hasPermission(4))
                .executes(context -> executeCommand(context.getSource())));
    }

    private static int executeCommand(CommandSourceStack source) {
        if (isCommandRunning) {
            LOGGER.warn("Commande spawntradenpc déjà en cours d'exécution.");
            source.sendFailure(Component.literal("La commande est déjà en cours d'exécution."));
            return 0;
        }

        isCommandRunning = true; // Verrouiller l'exécution

        try {
            if (source.getEntity() instanceof ServerPlayer) {
                // Étape 1 : Vérification du joueur
                if (!(source.getEntity() instanceof Player player)) {
                    source.sendFailure(Component.literal("[Step 1] This command can only be executed by a player."));
                    return 0;
                }
                source.sendSuccess(() -> Component.literal("[Step 1] Player verified successfully."), true);

                // Étape 2 : Vérification de l'item en main secondaire
                ItemStack offhandItem = player.getOffhandItem();
                if (offhandItem.isEmpty()) {
                    source.sendFailure(Component.literal("[Step 2] You must hold an item in your offhand to specify the trade category."));
                    return 0;
                }
                source.sendSuccess(() -> Component.literal("[Step 2] Offhand item found: " + offhandItem.getItem().getDescriptionId()), true);

                // Étape 3 : Récupération de l'ID de l'item pour la catégorie
                String category = offhandItem.getItem().builtInRegistryHolder().key().location().toString();
                source.sendSuccess(() -> Component.literal("[Step 3] Trade category determined: " + category), true);

                // Étape 4 : Création et ajout du PNJ
                BlockPos position = BlockPos.containing(source.getPosition());

                LOGGER.info("Création d'un PNJ via la commande spawntradenpc.");

                // Récupération d'un PNJ inactif
                String npcName = GlobalNpcManager.getRandomInactiveNpc(); // ON GET LE RANDOM NAME
                if (npcName == null) {
                    LOGGER.error("Aucun PNJ inactif disponible.");
                    source.sendFailure(Component.literal("No inactive NPCs available."));
                    return 0;
                }
                LOGGER.info("PNJ inactif aléatoire sélectionné : " + npcName);


                Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName); //ON GET LES DATAS CONSTANTE
                if (npcData == null) {
                    LOGGER.error("Données introuvables pour le PNJ : " + npcName);
                    source.sendFailure(Component.literal("Failed to retrieve NPC data for: " + npcName));
                    return 0;
                }
                LOGGER.info("Données du PNJ : " + npcData);




                // Création de l'entité NPC
                TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), source.getUnsidedLevel());
                if (npc == null) {
                    LOGGER.error("Impossible de créer une instance de TradeNpcEntity.");
                    source.sendFailure(Component.literal("Failed to create NPC instance."));
                    return 0;
                }
                TradeNpc modelNpc = new TradeNpc(npc.getUUID().toString(), npcName, npcData, category,position);

                ActiveNpcManager.printActiveNpcs();
                JsonTradeFileManager.addTradeNpcToJson(modelNpc); // Enregistrement dans le fichier JSON

                npc.setInitialized(false);
                npc.isCreated = true;
                npc.initializeNpcData();

                // Configuration du PNJ
//                npc.setTradeCategory(category);
//                npc.setPos(position.getX(), position.getY(), position.getZ());
//                npc.setPosition(position);
//                npc.setNpcId(npc.getStringUUID());
//                npc.setNpcName(npcName);
//                npc.setNpcData(npcData);
//                npc.setTexts((ArrayList<String>) npcData.getOrDefault("Texts", new ArrayList<>()));

//                // Vérification et définition de la texture
//                String texture = (String) npcData.getOrDefault("texture", null);
//                if (texture == null || texture.isEmpty()) {
//                    LOGGER.error("Texture manquante ou invalide pour le PNJ : " + npc.getNpcId());
//                    source.sendFailure(Component.literal("Invalid or missing texture for NPC: " + npc.getNpcName()));
//                    return 0;
//                }
//                npc.setTexture(texture);
//                npc.setTexture(texture);
//                LOGGER.info("Texture assignée au PNJ : " + texture);

//                // Vérification si un PNJ avec le même UUID ou position existe déjà VERIF QUIL N'Y EST PAS 2 PNJS
//                boolean npcExists = source.getLevel().getEntitiesOfClass(TradeNpcEntity.class, npc.getBoundingBox().inflate(1.0)).stream()
//                        .anyMatch(existingNpc -> existingNpc.getNpcId().equals(npc.getNpcId()));
//                if (npcExists) {
//                    LOGGER.warn("Un PNJ avec le même UUID existe déjà à cette position.");
//                    source.sendFailure(Component.literal("NPC with the same ID already exists at this location."));
//                    return 0;
//                }
//
//                // Ajout du PNJ au système
//
//
//
//                LOGGER.info("PNJ ajouté avec succès : " + npcName + " (ID: " + modelNpc.getNpcId() + ")");
//                source.sendSuccess(() -> Component.literal("[Step 4] Spawned trade NPC at " +
//                        position.getX() + ", " + position.getY() + ", " + position.getZ()), true);

            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la création ou de l'ajout du PNJ : ", e);
            source.sendFailure(Component.literal("[Step 4] Failed to spawn NPC: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        } finally {
            isCommandRunning = false; // Libération du verrou
        }

        source.sendSuccess(() -> Component.literal("[Final Step] Command executed successfully."), true);
        return 1;
    }
}
