package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.mojang.text2speech.Narrator.LOGGER;

public class TradeCommand {
	public static void registerCommand(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("trade")
						.requires(CommandPermissions::isAdmin)
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(TRADE_SUGGESTIONS)
								.executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
						)
		);
	}

	public static int execute(CommandSourceStack source, String inputName) {
		if (!CommandPermissions.isAdmin(source)) {
			source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
			return 0;
		}

		try {
			Level world = source.getLevel();
			double x = source.getPosition().x();
			double y = source.getPosition().y();
			double z = source.getPosition().z();
			Entity entity = source.getEntity();
			if (entity == null && world instanceof ServerLevel sl)
				entity = FakePlayerFactory.getMinecraft(sl);
			Direction direction = entity != null ? entity.getDirection() : Direction.DOWN;
			TradeCommandProcedure.execute(world, x, y, z, inputName, entity, "", "");
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
			return 0;
		}
	}

	private static final SuggestionProvider<CommandSourceStack> TRADE_SUGGESTIONS =
			(context, builder) -> {
				List<String> tradeNames = getTradeNamesFromFile();
				if (tradeNames.isEmpty()) {
					LOGGER.warn("Aucun trade trouvé dans le JSON.");
				} else {
					LOGGER.info("Suggestions de trades : " + tradeNames);
				}
				return suggest(tradeNames, builder);
			};

	private static List<String> getTradeNamesFromFile() {
		try {
			JsonRepository<Trade> repo = new JsonRepository<>(
					Paths.get(JsonFileManager.path), // chemin vers trades.json
					"trades",                        // clé racine
					Trade::fromJson,                 // désérialisation
					Trade::toJson                    // sérialisation
			);
			return repo.loadAll()
					.stream()
					.map(Trade::getName)
					.distinct()
					.toList();
		} catch (Exception e) {
			LOGGER.error("Erreur lecture noms de trades :", e);
			return List.of();
		}
	}

	private static CompletableFuture<Suggestions> suggest(
			List<String> suggestions, SuggestionsBuilder builder) {
		for (String s : suggestions) {
			builder.suggest(s);
		}
		return builder.buildFuture();
	}
}
