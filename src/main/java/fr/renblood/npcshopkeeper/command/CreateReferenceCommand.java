package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

import static fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager.createPriceReference;


@Mod.EventBusSubscriber
public class CreateReferenceCommand {

    @SubscribeEvent
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("createreference")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("item", ResourceLocationArgument.id()) // Utiliser ResourceLocationArgument pour l'item
                                .suggests(ITEM_SUGGESTIONS) // Suggestions d'items
                                .then(Commands.argument("min", IntegerArgumentType.integer())
                                        .then(Commands.argument("max", IntegerArgumentType.integer())
                                                .executes(arguments -> {
                                                    try {
                                                        Level world = arguments.getSource().getLevel();
                                                        double x = arguments.getSource().getPosition().x();
                                                        double y = arguments.getSource().getPosition().y();
                                                        double z = arguments.getSource().getPosition().z();
                                                        Entity entity = arguments.getSource().getEntity();
                                                        if (entity == null && world instanceof ServerLevel _servLevel)
                                                            entity = FakePlayerFactory.getMinecraft(_servLevel);
                                                        Direction direction = Direction.DOWN;
                                                        if (entity != null)
                                                            direction = entity.getDirection();
                                                        ResourceLocation item = ResourceLocationArgument.getId(arguments, "item");
                                                        int minPrice = IntegerArgumentType.getInteger(arguments, "min");
                                                        int maxPrice = IntegerArgumentType.getInteger(arguments, "max");
                                                        if(entity instanceof Player player) {
                                                            boolean success = createPriceReference(item.toString(), minPrice, maxPrice, player);
                                                            if (success) {
                                                                arguments.getSource().sendSuccess(() -> Component.translatable("command.npcshopkeeper.createreference.success", item.toString(), minPrice, maxPrice), true);
                                                                return 1;
                                                            } else {
                                                                arguments.getSource().sendFailure(Component.translatable("command.npcshopkeeper.createreference.failure"));
                                                                return 0;
                                                            }
                                                        }
                                                        return 0;
                                                    } catch (Exception e) {
                                                        arguments.getSource().sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
                                                        return 0;
                                                    }
                                                }))))
        );
    }

    // Suggestions dynamiques pour les items Minecraft
    private static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        return suggestMinecraftItems(builder); // Méthode personnalisée pour suggérer des items
    };

    // Méthode pour fournir une liste d'items Minecraft disponibles en suggestions
    private static CompletableFuture<Suggestions> suggestMinecraftItems(SuggestionsBuilder builder) {
        ForgeRegistries.ITEMS.getKeys().forEach(item -> builder.suggest(item.toString()));
        return builder.buildFuture();
    }

    // Logique de la commande
}