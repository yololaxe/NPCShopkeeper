package fr.renblood.npcshopkeeper.item;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
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
import java.util.Locale;
import java.util.stream.Collectors;

public class NpcShopkeerperWandItem extends Item {
	public NpcShopkeerperWandItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack wand = entity.getItemInHand(hand);

		// 1) If client, do nothing more here
		if (world.isClientSide) {
			// Let the server handle it
			return InteractionResultHolder.success(wand);
		}

		// Now we know we're on the logical server side
		if (!(entity instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(wand);
		}

		// 2) Offhand check
		ItemStack offhand = serverPlayer.getOffhandItem();
		String itemId = offhand.isEmpty() ? "" : offhand.getItem()
				.builtInRegistryHolder()
				.key()
				.location()
				.toString();

		Set<String> validCategories = RuntimeDataCache.getTrades().stream()
				.map(trade -> trade.getCategory())
				.filter(category -> category != null && !category.isBlank())
				.collect(Collectors.toSet());

		String matchedCategory = findMatchingCategory(validCategories, itemId);
		if (matchedCategory == null && offhand.hasCustomHoverName()) {
			matchedCategory = findMatchingCategory(validCategories, offhand.getHoverName().getString());
		}
		if (matchedCategory == null) matchedCategory = itemId;

		RightClickNpcShopkeeperWandProcedure.executeWithCategory(
				world,
				serverPlayer.getX(),
				serverPlayer.getY(),
				serverPlayer.getZ(),
				serverPlayer,
				matchedCategory
		);
		serverPlayer.displayClientMessage(
				Component.literal("Configurez une route de commerce ou une route menu d'auberge."),
				true
		);

		return InteractionResultHolder.success(wand);
	}

	private static String findMatchingCategory(Set<String> validCategories, String itemId) {
		String normalizedItemId = normalizeCategory(itemId);
		for (String category : validCategories) {
			if (normalizeCategory(category).equals(normalizedItemId)) {
				return category;
			}
		}
		return null;
	}

	private static String normalizeCategory(String category) {
		if (category == null) {
			return "";
		}

		String normalized = category.trim().toLowerCase(Locale.ROOT);
		int namespaceSeparator = normalized.indexOf(':');
		if (namespaceSeparator >= 0 && namespaceSeparator + 1 < normalized.length()) {
			return normalized.substring(namespaceSeparator + 1);
		}
		return normalized;
	}
}
