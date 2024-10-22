package fr.renblood.npcshopkeeper.procedures;

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

public class TradeCommandProcedure {

	// Cette méthode est appelée après que le joueur a exécuté une commande
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;

		// On vérifie que l'entité est bien un joueur côté serveur
		if (entity instanceof ServerPlayer serverPlayer) {
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
			TradePrepareProcedure.execute(entity);
		}
	}
}
