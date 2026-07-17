package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.renblood.npcshopkeeper.procedures.route.PointDefiningModeProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PointDefiningCommand {
    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("define")
                .requires(commandSource -> commandSource.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("remove").executes(ctx -> execute(ctx.getSource(), "remove")))
                .then(Commands.literal("cancel").executes(ctx -> execute(ctx.getSource(), "cancel")))
                .then(Commands.literal("finish").executes(ctx -> execute(ctx.getSource(), "finish")))
        );
    }

    public static int execute(CommandSourceStack source, String action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!PointDefiningModeProcedure.isInMode(player)) {
            player.displayClientMessage(Component.translatable("command.npcshopkeeper.define.not_in_mode"), false);
            return 1;
        }

        switch (action) {
            case "remove" -> PointDefiningModeProcedure.getMode(player).removeLastPoint();
            case "cancel" -> PointDefiningModeProcedure.getMode(player).cancel();
            case "finish" -> PointDefiningModeProcedure.getMode(player).finish();
            default -> {
                source.sendFailure(Component.literal("Action de route inconnue: " + action));
                return 0;
            }
        }
        return 1;
    }
}
