package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager; // for path
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import fr.renblood.npcshopkeeper.procedures.trade.TradePrepareProcedure;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH;

public class TradeCommandProcedure {
	private static final Logger LOGGER = LogManager.getLogger(TradeCommandProcedure.class);

	public static void execute(LevelAccessor world, double x, double y, double z,
							   String cleanTradeName, Entity entity,
							   String npcId, String npcName) {
		if (entity == null) {
			LOGGER.error("Entity is null. Cannot proceed with trade.");
			return;
		}

		// Charger dynamiquement la liste des noms de trades depuis trades.json
		List<String> tradeNames;
		try {
			JsonRepository<Trade> repo = new JsonRepository<>(
					Paths.get(PATH),
					"trades",
					Trade::fromJson,
					Trade::toJson
			);
			tradeNames = repo.loadAll().stream()
					.map(Trade::getName)
					.distinct()
					.collect(Collectors.toList());
			LOGGER.info("Trade names loaded successfully: {}", tradeNames);
		} catch (Exception e) {
			LOGGER.error("Failed to load trade names via JsonRepository.", e);
			return;
		}

		// Vérifier que c'est bien un ServerPlayer
		if (!(entity instanceof ServerPlayer serverPlayer)) {
			LOGGER.warn("Entity is not a ServerPlayer. Trade command cannot be executed.");
			return;
		}

		LOGGER.info("Executing trade command for player: {}", serverPlayer.getName().getString());

		if (!tradeNames.contains(cleanTradeName)) {
			serverPlayer.displayClientMessage(
					Component.literal("Ce trade n'existe pas : " + cleanTradeName),
					false
			);
			LOGGER.warn("Trade '{}' not found. Available: {}", cleanTradeName, tradeNames);
			return;
		}

		BlockPos blockPos = BlockPos.containing(x, y, z);
		LOGGER.info("Trade '{}' found. Opening TradeMenu at {}", cleanTradeName, blockPos);

		try {
			NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Trade");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
					LOGGER.info("Creating TradeMenu for player {} at {}", serverPlayer.getName().getString(), blockPos);
					FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
					buf.writeBlockPos(blockPos)
							.writeUtf(cleanTradeName)
							.writeUtf(npcId)
							.writeUtf(npcName);
					return new TradeMenu(id, playerInventory, buf);
				}
			}, buffer -> {
				buffer.writeBlockPos(blockPos)
						.writeUtf(cleanTradeName)
						.writeUtf(npcId)
						.writeUtf(npcName);
			});

			LOGGER.info("Trade menu opened successfully for player: {}", serverPlayer.getName().getString());
			TradePrepareProcedure.execute(entity, cleanTradeName, npcId, npcName);

		} catch (Exception e) {
			LOGGER.error("Failed to open trade menu for player: {}", serverPlayer.getName().getString(), e);
		}
	}
}
