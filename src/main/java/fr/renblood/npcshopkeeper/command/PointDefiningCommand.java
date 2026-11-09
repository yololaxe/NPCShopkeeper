package fr.renblood.npcshopkeeper.command;

import fr.renblood.npcshopkeeper.procedures.route.PointDefiningModeProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
public class PointDefiningCommand {
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
