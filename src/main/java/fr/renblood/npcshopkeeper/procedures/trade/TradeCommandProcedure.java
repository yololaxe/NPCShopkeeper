package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import io.netty.buffer.Unpooled;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TradeCommandProcedure {

	private static final Logger LOGGER = LogManager.getLogger(TradeCommandProcedure.class);

	// Cette méthode est appelée après que le joueur a exécuté une commande
	public static void execute(LevelAccessor world, double x, double y, double z, String cleanTradeName, Entity entity, String npcId, String npcName) {
		if (entity == null) {
			LOGGER.error("Entity is null. Cannot proceed with trade.");
			return;
		}

		// Lire les noms des trades
		List<String> tradeNames;
		try {
			tradeNames = JsonTradeFileManager.readTradeNames();
			LOGGER.info("Trade names loaded successfully: {}", tradeNames);
		} catch (Exception e) {
			LOGGER.error("Failed to read trade names from JsonTradeFileManager.", e);
			return;
		}

		// Vérifier que l'entité est bien un joueur côté serveur
		if (entity instanceof ServerPlayer serverPlayer) {
			LOGGER.info("Executing trade command for player: {}", serverPlayer.getName().getString());

			if (tradeNames.contains(cleanTradeName)) {
				BlockPos blockPos = BlockPos.containing(x, y, z);
				LOGGER.info("Trade '{}' found in the list. Proceeding to open menu.", cleanTradeName);

				// Utilisation de NetworkHooks pour ouvrir l'interface de trading pour le joueur
				try {
					NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
						@Override
						public Component getDisplayName() {
							// Le nom du menu affiché dans l'interface
							return Component.literal("Trade");
						}

						@Override
						public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
							// Création du TradeMenu avec les coordonnées du bloc où la commande a été exécutée
							LOGGER.info("Creating TradeMenu for player: {}, blockPos: {}", player.getName().getString(), blockPos);
							return new TradeMenu(id, playerInventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(blockPos));
						}
					}, buffer -> buffer.writeBlockPos(blockPos)); // Écriture de la position du bloc dans le buffer réseau

					LOGGER.info("Trade menu opened successfully for player: {}", serverPlayer.getName().getString());

					// Appeler la procédure de préparation du trade
					TradePrepareProcedure.execute(entity, cleanTradeName, npcId, npcName);
				} catch (Exception e) {
					LOGGER.error("Failed to open trade menu for player: {}", serverPlayer.getName().getString(), e);
				}
			} else {
				serverPlayer.displayClientMessage(Component.literal("Ce trade n'existe pas: " + cleanTradeName), false);
				LOGGER.warn("Trade '{}' not found in the list. Available trades: {}", cleanTradeName, tradeNames);
			}
		} else {
			LOGGER.warn("Entity is not a ServerPlayer. Trade command cannot be executed.");
		}
	}
}
