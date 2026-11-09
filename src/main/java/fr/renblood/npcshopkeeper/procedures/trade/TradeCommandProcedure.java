package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
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

public class TradeCommandProcedure {
	private static final Logger LOGGER = LogManager.getLogger(TradeCommandProcedure.class);

	public static void execute(LevelAccessor world, double x, double y, double z,
							   String cleanTradeName, Entity entity,
							   String npcId, String npcName) {
		if (entity == null) {
			LOGGER.error("Entity is null. Cannot proceed with trade.");
			return;
		}

        if (!(entity instanceof ServerPlayer serverPlayer)) return;
        if (fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache.getTrade(cleanTradeName) == null) {
            serverPlayer.displayClientMessage(Component.literal("Cette offre n’est plus disponible. Demande à l’équipe de vérifier le catalogue.")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
            return;
        }

		BlockPos blockPos = BlockPos.containing(x, y, z);
		LOGGER.debug("Trade '{}' found. Opening TradeMenu at {}", cleanTradeName, blockPos);

		try {
			NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Trade");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
					LOGGER.debug("Creating TradeMenu for player {} at {}", serverPlayer.getName().getString(), blockPos);
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

			LOGGER.debug("Trade menu opened successfully for player: {}", serverPlayer.getName().getString());
			
			// Appel différé de TradePrepareProcedure pour laisser le temps au client d'ouvrir le GUI
			// 2 ticks = 100ms, devrait suffire pour la synchro sans casser l'ID
            AbstractContainerMenu expectedMenu = serverPlayer.containerMenu;
            Npcshopkeeper.queueServerWork(2, () -> {
                if (!serverPlayer.hasDisconnected() && serverPlayer.containerMenu == expectedMenu) {
                    TradePrepareProcedure.execute(entity, cleanTradeName, npcId, npcName);
                }
            });

		} catch (Exception e) {
			LOGGER.error("Failed to open trade menu for player: {}", serverPlayer.getName().getString(), e);
		}
	}
}
