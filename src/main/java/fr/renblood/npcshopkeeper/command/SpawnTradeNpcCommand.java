package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.GlobalNpcManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod.EventBusSubscriber
public class SpawnTradeNpcCommand {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("spawntradenpc").requires(s -> s.hasPermission(4))
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    Level world = source.getUnsidedLevel();

                    // Limiter l'exécution au serveur
                    if (world.isClientSide()) {
                        LOGGER.warn("Command executed on the client side. Ignoring...");
                        return 0;
                    }

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
                    try {

                        BlockPos position = BlockPos.containing(source.getPosition());

                        // Utiliser la méthode de Forge pour créer une entité
                        if (!world.isClientSide) {
                            LOGGER.info("Création d'un PNJ via la commande spawntradenpc.");
                            TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                            //LOGGER.info("Constructeur de TradeNpcEntity appelé : " + npc.getUUID());

                            if (npc == null) {
                                source.sendFailure(Component.literal("Failed to create NPC instance."));
                                return 0;
                            }

                            npc.setTradeCategory(category);
                            npc.setPos(position.getX(), position.getY(), position.getZ());

                            if(npc.getNpcName() != "null")
                                world.addFreshEntity(npc);
                            else
                                LOGGER.info("PNJ null : " + npc.getUUID());

                            source.sendSuccess(() -> Component.literal("[Step 4] Spawned trade NPC at " +
                                    position.getX() + ", " + position.getY() + ", " + position.getZ()), true);

                        }
                        else {
                            source.sendFailure(Component.literal("[Step 4] Failed to spawn NPC: "));
                        }
                    } catch (Exception e) {
                        source.sendFailure(Component.literal("[Step 4] Failed to spawn NPC: " + e.getMessage()));
                        e.printStackTrace();
                        return 0;
                    }

                    source.sendSuccess(() -> Component.literal("[Final Step] Command executed successfully."), true);
                    return 1;
                }));
    }
}
