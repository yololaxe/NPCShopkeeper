package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.renblood.npcshopkeeper.procedures.trade.CreateTradeCommandProcedure;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class CreateTradeCommand {
    public static void registerCommand(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createtrade").executes(arguments -> execute(arguments.getSource())));
    }

    public static int execute(CommandSourceStack source) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.literal("Vous n'avez pas la permission d'ouvrir la creation de trade."));
            return 0;
        }

        try {
            Level world = source.getUnsidedLevel();
            double x = source.getPosition().x();
            double y = source.getPosition().y();
            double z = source.getPosition().z();
            Entity entity = source.getEntity();
            if (entity == null && world instanceof ServerLevel _servLevel)
                entity = FakePlayerFactory.getMinecraft(_servLevel);
            Direction direction = Direction.DOWN;
            if (entity != null)
                direction = entity.getDirection();
            CreateTradeCommandProcedure.execute(world, x, y, z, entity);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
            return 0;
        }
    }
}
