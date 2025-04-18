package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import java.util.concurrent.CompletableFuture;
import java.util.List;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod.EventBusSubscriber
public class TradeCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("trade")
				// Ajout des suggestions pour le nom des trades
				.then(Commands.argument("name", StringArgumentType.word())
						.suggests(TRADE_SUGGESTIONS) // Ajouter les suggestions dynamiques ici
						.executes(arguments -> {
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

							TradeCommandProcedure.execute(world, x, y, z, arguments.getInput().substring(6), entity, "", "");
							return 0;
						})));
	}

	// SuggestionProvider pour donner les noms des trades dans la commande
	private static final SuggestionProvider<CommandSourceStack> TRADE_SUGGESTIONS = (context, builder) -> {
		List<String> tradeNames = getTradeNamesFromFile();

		if (tradeNames.isEmpty()) {
			LOGGER.warn("Aucun trade trouvé dans le fichier JSON. (TradeCommand)" );

		} else {
			LOGGER.info("Suggestions de trades trouvées : " + tradeNames);
		}



		return suggest(tradeNames, builder);
	};

	// Méthode pour récupérer les noms des trades depuis le fichier JSON
	private static List<String> getTradeNamesFromFile() {
		try {
			List<String> tradeNames = JsonTradeFileManager.readTradeNames();

			return tradeNames;
		} catch (Exception e) {
			LOGGER.error("Erreur lors de la lecture des noms des trades dans le fichier JSON.", e);
			return List.of(); // Retourner une liste vide en cas d'erreur
		}
	}

	// Utilitaire pour ajouter des suggestions
	private static CompletableFuture<Suggestions> suggest(List<String> suggestions, SuggestionsBuilder builder) {
		for (String suggestion : suggestions) {
			builder.suggest(suggestion);
		}
		return builder.buildFuture();
	}
}
