package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.concurrent.CompletableFuture;

import static fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager.createPriceReference;

@Mod.EventBusSubscriber
public class CreateReferenceCommand {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("createreference")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                .suggests(ITEM_SUGGESTIONS)
                                .then(Commands.argument("min", IntegerArgumentType.integer())
                                        .then(Commands.argument("max", IntegerArgumentType.integer())
                                                .executes(arguments -> execute(
                                                        arguments.getSource(),
                                                        ResourceLocationArgument.getId(arguments, "item"),
                                                        IntegerArgumentType.getInteger(arguments, "min"),
                                                        IntegerArgumentType.getInteger(arguments, "max"))))))
        );
    }

    public static int execute(CommandSourceStack source, ResourceLocation item, int minPrice, int maxPrice) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Cette commande doit etre executee par un joueur."));
            return 0;
        }

        try {
            boolean success = createPriceReference(item.toString(), minPrice, maxPrice, player);
            if (success) {
                source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.createreference.success", item.toString(), minPrice, maxPrice), true);
                return 1;
            }

            source.sendFailure(Component.translatable("command.npcshopkeeper.createreference.failure"));
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
            return 0;
        }
    }

    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> suggestMinecraftItems(builder);

    private static CompletableFuture<Suggestions> suggestMinecraftItems(SuggestionsBuilder builder) {
        ForgeRegistries.ITEMS.getKeys().forEach(item -> builder.suggest(item.toString()));
        return builder.buildFuture();
    }
}
