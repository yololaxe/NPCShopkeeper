package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.manager.trade.XpReferenceManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber
public class SetXpReferenceCommand {

    private static final List<String> JOBS = List.of(
            "lumberjack", "naval_architect", "artisan", "carpenter", "miner",
            "blacksmith", "glassmaker", "mason", "farmer", "breeder",
            "fisherman", "innkeeper", "guard", "merchant", "transporter",
            "explorer", "bestiary", "banker", "politician", "builder"
    );

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("set_xp_reference")
                .requires(CommandPermissions::isAdmin)
                .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(JOBS, builder))
                                .then(Commands.argument("min", FloatArgumentType.floatArg(0))
                                        .then(Commands.argument("max", FloatArgumentType.floatArg(0))
                                                .executes(context -> {
                                                    Item item = ItemArgument.getItem(context, "item").getItem();
                                                    String itemName = ForgeRegistries.ITEMS.getKey(item).toString();
                                                    return execute(
                                                            context.getSource(),
                                                            itemName,
                                                            StringArgumentType.getString(context, "job"),
                                                            FloatArgumentType.getFloat(context, "min"),
                                                            FloatArgumentType.getFloat(context, "max"));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    public static int execute(CommandSourceStack source, String itemName, String job, float min, float max) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        try {
            if (!JOBS.contains(job)) {
                source.sendFailure(Component.translatable("command.npcshopkeeper.set_xp_reference.unknown_job", job));
                return 0;
            }

            if (min > max) {
                source.sendFailure(Component.translatable("command.npcshopkeeper.set_xp_reference.min_greater_than_max"));
                return 0;
            }

            boolean success = XpReferenceManager.createOrUpdateXpReference(itemName, job, min, max);
            if (success) {
                source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.set_xp_reference.success", itemName, job, min, max), true);
                return 1;
            }

            source.sendFailure(Component.translatable("command.npcshopkeeper.set_xp_reference.error"));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
            return 0;
        }
    }
}
