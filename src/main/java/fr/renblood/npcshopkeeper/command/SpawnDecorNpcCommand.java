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
                .requires(CommandPermissions::isAdmin)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(NPC_NAME_SUGGESTIONS)
                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, "name"))))
        );
    }

    public static int execute(CommandSourceStack source, String requestedName) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        try {
            String npcName = resolveNpcName(requestedName);
            ServerLevel world = source.getLevel();
            BlockPos pos = BlockPos.containing(source.getPosition());

            Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName);
            if (npcData == null) {
                source.sendFailure(Component.translatable("command.npcshopkeeper.spawn_decor_npc.unknown_npc", npcName));
                return 0;
            }

            TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
            npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            TradeNpc modelNpc = new TradeNpc(npcName, npcData, "decor", pos);
            modelNpc.setRouteNpc(false);
            modelNpc.setNpcId(npc.getStringUUID());
            npc.setTradeNpc(modelNpc);

            npc.getPersistentData().putBoolean("IsDecor", true);
            npc.getPersistentData().putString("DecorName", npcName);

            NpcSpawn spawn = new NpcSpawn();
            spawn.spawn_id = UUID.randomUUID().toString();
            spawn.npc_id = resolveNpcId(npcName, npcData);
            spawn.world = world.dimension().location().toString();
            spawn.x = pos.getX() + 0.5D;
            spawn.y = pos.getY();
            spawn.z = pos.getZ() + 0.5D;
            spawn.spawn_rule = "STATIC";
            spawn.active = true;

            if (!MedievalCoinsIntegration.createNpcSpawn(spawn)) {
                source.sendFailure(Component.literal("Le backend a refuse la creation du spawn PNJ."));
                return 0;
            }

            npc.setApiNpcSpawnId(spawn.spawn_id);
            npc.getPersistentData().putString("ApiNpcType", "DECO");
            npc.getPersistentData().putBoolean("ApiIsShopkeeper", false);
            world.addFreshEntity(npc);

            source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.spawn_decor_npc.success", npcName), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
            return 0;
        }
    }

    public static final SuggestionProvider<CommandSourceStack> NPC_NAME_SUGGESTIONS = (context, builder) -> SharedSuggestionProvider.suggest(
            GlobalNpcManager.getAllNpcData().keySet().stream().map(name -> name.replace(' ', '_')),
            builder
    );

    private static String resolveNpcName(String requestedName) {
        if (GlobalNpcManager.getNpcData(requestedName) != null) return requestedName;
        return requestedName.replace('_', ' ');
    }

    private static String resolveNpcId(String npcName, Map<String, Object> npcData) {
        Object configuredId = npcData.get("npcId");
        if (configuredId instanceof String npcId && !npcId.isBlank()) {
            return npcId;
        }

        String generated = npcName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return generated.isEmpty() ? "npc-" + System.currentTimeMillis() : generated;
    }
}
