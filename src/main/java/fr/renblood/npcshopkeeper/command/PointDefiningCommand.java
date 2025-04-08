package fr.renblood.npcshopkeeper.command;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.procedures.PointDefiningModeProcedure;
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
        event.getDispatcher().register(Commands.literal("define")
                .requires(commandSource -> commandSource.getEntity() instanceof ServerPlayer) // S'assure que seule une entité serveur peut exécuter la commande
                .then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (PointDefiningModeProcedure.isInMode(player)) {
                        PointDefiningModeProcedure.getMode(player).removeLastPoint();
                    } else {
                        player.displayClientMessage(Component.literal("Vous n'êtes pas en mode définir des points."), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("cancel").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (PointDefiningModeProcedure.isInMode(player)) {
                        PointDefiningModeProcedure.getMode(player).cancel();
                    } else {
                        player.displayClientMessage(Component.literal("Vous n'êtes pas en mode définir des points."), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("finish").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (PointDefiningModeProcedure.isInMode(player)) {
                        PointDefiningModeProcedure.getMode(player).finish();
                    } else {
                        player.displayClientMessage(Component.literal("Vous n'êtes pas en mode définir des points."), false);
                    }
                    return 1;
                }))
        );
    }
}
