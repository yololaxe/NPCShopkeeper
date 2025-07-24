package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;
import net.minecraft.core.Direction;
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
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH;

@Mod.EventBusSubscriber
public class TradeCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(
				Commands.literal("trade")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests(TRADE_SUGGESTIONS)
								.executes(ctx -> {
									Level world = ctx.getSource().getLevel();
									double x = ctx.getSource().getPosition().x();
									double y = ctx.getSource().getPosition().y();
									double z = ctx.getSource().getPosition().z();
									Entity entity = ctx.getSource().getEntity();
									if (entity == null && world instanceof ServerLevel sl)
										entity = FakePlayerFactory.getMinecraft(sl);
									Direction direction = entity != null ? entity.getDirection() : Direction.DOWN;
									String inputName = StringArgumentType.getString(ctx, "name");
									TradeCommandProcedure.execute(world, x, y, z,
											inputName, entity, "", "");
									return 0;
								})
						)
		);
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
					Paths.get(PATH), // chemin vers trades.json
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
