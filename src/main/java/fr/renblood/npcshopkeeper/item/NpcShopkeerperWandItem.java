package fr.renblood.npcshopkeeper.item;

import fr.renblood.npcshopkeeper.data.io.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.procedures.route.RightClickNpcShopkeeperWandProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;

import java.util.Set;

public class NpcShopkeerperWandItem extends Item {
	public NpcShopkeerperWandItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack offHandItem = entity.getOffhandItem(); // Récupère l'objet dans la main secondaire

		// Vérifier si l'objet en main secondaire est vide
		if (offHandItem.isEmpty()) {
			if (!world.isClientSide) { // Vérifier côté serveur pour éviter les duplications
				entity.displayClientMessage(Component.literal("Vous devez tenir un objet dans votre main secondaire."), true);
			}
			return InteractionResultHolder.pass(entity.getItemInHand(hand));
		}

		// Vérifier si l'objet appartient à une catégorie valide
		Set<String> validCategories = JsonTradeFileManager.getAllCategory();
		String itemId = offHandItem.getItem().builtInRegistryHolder().key().location().toString(); // ID complet de l'objet
		if (!validCategories.contains(itemId)) {
			if (!world.isClientSide) {
				entity.displayClientMessage(Component.literal("Cet objet n'est pas valide pour créer une route commerciale."), true);
			}
			return InteractionResultHolder.pass(entity.getItemInHand(hand));
		}

		// Si tout est valide, ouvrir le GUI en passant la catégorie
		if (!world.isClientSide) {
			if (entity instanceof ServerPlayer serverPlayer) {
				// Appeler la procédure en lui passant la catégorie
				RightClickNpcShopkeeperWandProcedure.executeWithCategory(world, entity.getX(), entity.getY(), entity.getZ(), serverPlayer, itemId);
				serverPlayer.displayClientMessage(Component.literal("Début de la création de la route commerciale."), true);
			}
		}

		return InteractionResultHolder.success(entity.getItemInHand(hand));
	}
}
