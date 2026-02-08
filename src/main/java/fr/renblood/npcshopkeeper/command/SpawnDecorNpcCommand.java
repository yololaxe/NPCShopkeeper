package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
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

import java.util.Map;

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

                                // 2. Créer le modèle TradeNpc (sans trade, hors route)
                                TradeNpc modelNpc = new TradeNpc(npcName, npcData, "decor", pos);
                                modelNpc.setRouteNpc(false); // Ce n'est pas un PNJ de route
                                
                                // 3. Créer l'entité
                                TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                                npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                
                                // 4. Associer le modèle
                                modelNpc.setNpcId(npc.getStringUUID());
                                npc.setTradeNpc(modelNpc);
                                
                                npc.getPersistentData().putBoolean("IsDecor", true);
                                npc.getPersistentData().putString("DecorName", npcName);

                                world.addFreshEntity(npc);

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
