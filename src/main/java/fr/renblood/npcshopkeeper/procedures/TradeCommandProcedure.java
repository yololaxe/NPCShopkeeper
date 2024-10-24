package fr.renblood.npcshopkeeper.procedures;

import com.mojang.brigadier.context.CommandContext;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import net.minecraft.commands.CommandSourceStack;
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

import java.util.List;

public class TradeCommandProcedure {

	// Cette méthode est appelée après que le joueur a exécuté une commande
	public static void execute(LevelAccessor world, double x, double y, double z, CommandContext<CommandSourceStack>arguments , Entity entity) {
		if (entity == null)
			return;

		String cleanTradeName = arguments.getInput().substring(6);
		List<String> tradeNames = JsonTradeFileManager.readTradeNames();
		// On vérifie que l'entité est bien un joueur côté serveur
		if (entity instanceof ServerPlayer serverPlayer) {

			if( tradeNames.contains(cleanTradeName)) {
				BlockPos blockPos = BlockPos.containing(x, y, z);

				// Utilisation de NetworkHooks pour ouvrir l'interface de trading pour le joueur
				NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
					@Override
					public Component getDisplayName() {
						// Le nom du menu affiché dans l'interface
						return Component.literal("Trade");
					}

					@Override
					public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
						// Création du TradeMenu avec les coordonnées du bloc où la commande a été exécutée
						return new TradeMenu(id, playerInventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(blockPos));
					}
				}, buffer -> buffer.writeBlockPos(blockPos));  // Écriture de la position du bloc dans le buffer réseau
				TradePrepareProcedure.execute(entity, cleanTradeName);
			} else {
				serverPlayer.displayClientMessage(Component.literal("Ce trade n'existe pas"+tradeNames+"" +cleanTradeName), false);
			}
		}

	}
}
