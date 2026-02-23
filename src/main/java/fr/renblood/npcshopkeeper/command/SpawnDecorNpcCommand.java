package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber
public class SpawnDecorNpcCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawn_decor_npc")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(NPC_NAME_SUGGESTIONS)
                        .executes(context -> {
                            try {
                                String npcName = StringArgumentType.getString(context, "name");
                                CommandSourceStack source = context.getSource();
                                ServerLevel world = source.getLevel();
                                BlockPos pos = BlockPos.containing(source.getPosition());

                                // 1. Récupérer les données du PNJ
                                Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName);
                                if (npcData == null) {
                                    source.sendFailure(Component.translatable("command.npcshopkeeper.spawn_decor_npc.unknown_npc", npcName));
                                    return 0;
                                }

                                // 2. Créer l'entité
                                TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                                npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                
                                // 3. Configurer l'entité (pour affichage immédiat)
                                TradeNpc modelNpc = new TradeNpc(npcName, npcData, "decor", pos);
                                modelNpc.setRouteNpc(false);
                                modelNpc.setNpcId(npc.getStringUUID());
                                npc.setTradeNpc(modelNpc);
                                
                                npc.getPersistentData().putBoolean("IsDecor", true);
                                npc.getPersistentData().putString("DecorName", npcName);

                                world.addFreshEntity(npc);
                                
                                // 4. Enregistrer le spawn dans le backend via l'API
                                NpcSpawn spawn = new NpcSpawn();
                                spawn.spawn_id = UUID.randomUUID().toString();
                                
                                // Récupération de l'ID correct (slug)
                                // On essaie de le trouver dans les données si disponible, sinon on le régénère comme à la création
                                String npcId = (String) npcData.getOrDefault("npcId", null);
                                if (npcId == null) {
                                    // Fallback : régénération du slug comme dans CreateNpcButtonMessage
                                    npcId = npcName.toLowerCase(Locale.ROOT)
                                            .replaceAll("[^a-z0-9]+", "-")
                                            .replaceAll("^-|-$", "");
                                    if (npcId.isEmpty()) npcId = "npc-" + System.currentTimeMillis();
                                }
                                spawn.npc_id = npcId;

                                spawn.world = world.dimension().location().toString();
                                spawn.x = pos.getX();
                                spawn.y = pos.getY();
                                spawn.z = pos.getZ();
                                spawn.spawn_rule = "STATIC";
                                spawn.active = true;
                                
                                MedievalCoinsIntegration.createNpcSpawn(spawn);

                                source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.spawn_decor_npc.success", npcName), true);
                                return 1;
                            } catch (Exception e) {
                                context.getSource().sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
                                return 0;
                            }
                        })
                )
        );
    }

    private static final SuggestionProvider<CommandSourceStack> NPC_NAME_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(GlobalNpcManager.getAllNpcData().keySet(), builder);
    };
}
