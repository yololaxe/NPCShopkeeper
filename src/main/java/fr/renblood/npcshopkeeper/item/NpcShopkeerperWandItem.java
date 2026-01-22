package fr.renblood.npcshopkeeper.item;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager; // pour path
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
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
import java.util.stream.Collectors;
import java.nio.file.Paths;

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
		if (offhand.isEmpty()) {
			serverPlayer.displayClientMessage(
					Component.literal("Vous devez tenir un objet dans votre main secondaire."),
					true
			);
			return InteractionResultHolder.pass(wand);
		}

		String itemId = offhand.getItem()
				.builtInRegistryHolder()
				.key()
				.location()
				.toString();

		// 3) Load valid categories **only once**, on the server
		Set<String> validCategories;
		try {
			// Utilisation de OnServerStartedManager.PATH au lieu de JsonFileManager.path
			String tradePath = OnServerStartedManager.PATH;
			if (tradePath == null) {
				Npcshopkeeper.LOGGER.error("Le chemin vers trades.json n'est pas initialisé !");
				return InteractionResultHolder.fail(wand);
			}
			
			validCategories = new JsonRepository<>(
					Paths.get(tradePath),
					"trades",
					Trade::fromJson,
					Trade::toJson
			).loadAll().stream()
					.map(Trade::getCategory)
					.collect(Collectors.toSet());
		} catch (Exception e) {
			// Log to server console — the client needn't see this
			Npcshopkeeper.LOGGER.error("Impossible de charger trades.json pour récupérer les catégories", e);
			serverPlayer.displayClientMessage(
					Component.literal("Erreur interne lors du chargement des catégories."),
					true
			);
			return InteractionResultHolder.fail(wand);
		}

		// 4) Check category
		if (!validCategories.contains(itemId)) {
			serverPlayer.displayClientMessage(
					Component.literal("Cet objet n'est pas valide pour créer une route commerciale."),
					true
			);
			return InteractionResultHolder.pass(wand);
		}

		// 5) Finally do your normal route-creation logic
		RightClickNpcShopkeeperWandProcedure.executeWithCategory(
				world,
				serverPlayer.getX(),
				serverPlayer.getY(),
				serverPlayer.getZ(),
				serverPlayer,
				itemId
		);
		serverPlayer.displayClientMessage(
				Component.literal("Début de la création de la route commerciale."),
				true
		);

		return InteractionResultHolder.success(wand);
	}
}
