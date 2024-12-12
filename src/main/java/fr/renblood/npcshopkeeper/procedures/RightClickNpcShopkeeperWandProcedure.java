package fr.renblood.npcshopkeeper.procedures;

import fr.renblood.npcshopkeeper.world.inventory.NpcShopkeeperWandGuiMenu;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public class RightClickNpcShopkeeperWandProcedure {
	public static void executeWithCategory(LevelAccessor world, double x, double y, double z, ServerPlayer serverPlayer, String category) {
		BlockPos blockPos = BlockPos.containing(x, y, z);

		// Ouvrir le GUI pour le joueur et transmettre la catégorie
		NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
			@Override
			public Component getDisplayName() {
				return Component.literal("NpcShopkeeper Wand");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
				return new NpcShopkeeperWandGuiMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer())
						.writeBlockPos(blockPos)
						.writeUtf(category)); // Ajouter la catégorie au buffer
			}
		}, buffer -> {
			buffer.writeBlockPos(blockPos);
			buffer.writeUtf(category); // Ajouter la catégorie au buffer
		});
	}
}
