package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
                .requires(source -> source.hasPermission(2)) // Permission niveau 2 (OP)
                .then(Commands.literal("log")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    Npcshopkeeper.DEBUG_MODE = enabled;
                                    context.getSource().sendSuccess(() -> Component.translatable("command.npcshopkeeper.debug.log", (enabled ? "ENABLED" : "DISABLED")), true);
                                    return 1;
                                })
                        )
                        .executes(context -> {
                            // Toggle si aucun argument n'est fourni
                            Npcshopkeeper.DEBUG_MODE = !Npcshopkeeper.DEBUG_MODE;
                            context.getSource().sendSuccess(() -> Component.translatable("command.npcshopkeeper.debug.log", (Npcshopkeeper.DEBUG_MODE ? "ENABLED" : "DISABLED")), true);
                            return 1;
                        })
                )
                .then(Commands.literal("add_dummy_roads")
                        .executes(context -> {
                            if (Npcshopkeeper.DEBUG_MODE) {
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
                                context.getSource().sendSuccess(() -> Component.translatable("command.npcshopkeeper.debug.dummy_roads_added"), true);
                            } else {
                                context.getSource().sendFailure(Component.translatable("command.npcshopkeeper.debug.enable_first"));
                            }
                            return 1;
                        })
                )
        );
    }
}
