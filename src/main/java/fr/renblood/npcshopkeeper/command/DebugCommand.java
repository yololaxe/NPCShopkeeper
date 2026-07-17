package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class DebugCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("npcshopkeeper_debug")
                .requires(CommandPermissions::isAdmin)
                .then(Commands.literal("log")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> setLog(context.getSource(), BoolArgumentType.getBool(context, "enabled"))))
                        .executes(context -> toggleLog(context.getSource()))
                )
                .then(Commands.literal("add_dummy_roads")
                        .executes(context -> addDummyRoads(context.getSource())))
        );
    }

    public static int setLog(CommandSourceStack source, boolean enabled) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        Npcshopkeeper.DEBUG_MODE = enabled;
        source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.debug.log", enabled ? "ENABLED" : "DISABLED"), true);
        return 1;
    }

    public static int toggleLog(CommandSourceStack source) {
        return setLog(source, !Npcshopkeeper.DEBUG_MODE);
    }

    public static int addDummyRoads(CommandSourceStack source) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        if (!Npcshopkeeper.DEBUG_MODE) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.debug.enable_first"));
            return 1;
        }

        for (int i = 1; i <= 30; i++) {
            String category = (i % 3 == 0) ? "minecraft:apple" : (i % 3 == 1) ? "minecraft:iron_ingot" : "minecraft:diamond";
            Npcshopkeeper.COMMERCIAL_ROADS.add(new CommercialRoad(
                    "dummy_id_" + i,
                    "Dummy Road " + i,
                    category,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    60,
                    120
            ));
        }
        source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.debug.dummy_roads_added"), true);
        return 1;
    }
}
