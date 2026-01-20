package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.trade.TradeResult;
import fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TradeProcedure {
	private static boolean isProcessingTrade = false;
	private static final Logger LOGGER = LogManager.getLogger(TradeProcedure.class);

	// ── Helpers ─────────────────────────────────────────────────────────

	private static Trade getTradeByName(String name) {
		JsonRepository<Trade> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.path),
				"trades",
				Trade::fromJson,
				Trade::toJson
		);
		return repo.loadAll().stream()
				.filter(t -> t.getName().equals(name))
				.findFirst().orElse(null);
	}

	private static TradeHistory getTradeHistoryById(String id) {
		JsonRepository<TradeHistory> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.pathHistory),
				"history",
				TradeHistory::fromJson,
				TradeHistory::toJson
		);
		return repo.loadAll().stream()
				.filter(h -> h.getId().equals(id))
				.findFirst().orElse(null);
	}

	private static void markTradeAsFinished(ServerPlayer player, String id) {
		JsonRepository<TradeHistory> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.pathHistory),
				"history",
				TradeHistory::fromJson,
				TradeHistory::toJson
		);
		List<TradeHistory> all = new ArrayList<>(repo.loadAll());
		for (TradeHistory h : all) {
			if (h.getId().equals(id)) {
				h.setFinished(true);
				break;
			}
		}
		repo.saveAll(all);
	}

	// ── Main execution ───────────────────────────────────────────────────

	@SuppressWarnings("unchecked")
	public static void execute(Entity entity) {
		if (entity == null || isProcessingTrade) return;
		isProcessingTrade = true;

		try {
			if (entity instanceof ServerPlayer player
					&& player.containerMenu instanceof Supplier<?> sup
					&& sup.get() instanceof Map<?, ?> rawSlots) {

				Map<Integer, Slot> slots = (Map<Integer, Slot>) rawSlots;

				// Read trade name + id from slot 12
				ItemStack categoryStack = slots.get(12).getItem();
				if (categoryStack.isEmpty() || !categoryStack.hasCustomHoverName()) {
					LOGGER.warn("Slot 12 vide ou sans nom, impossible de récupérer l'ID du trade.");
					return;
				}

				String label = categoryStack.getHoverName().getString();
				// Le format est "NomDuTrade UUID"
				// On cherche le dernier espace pour séparer le nom de l'UUID
				int lastSpaceIndex = label.lastIndexOf(' ');
				if (lastSpaceIndex == -1) {
					// Fallback si le format est différent (ex: avec crochets)
					label = label.replace("[","").replace("]","");
					lastSpaceIndex = label.lastIndexOf(' ');
				}
				
				String tradeName, tradeId;
				if (lastSpaceIndex != -1) {
					tradeName = label.substring(0, lastSpaceIndex);
					tradeId = label.substring(lastSpaceIndex + 1);
				} else {
					// Fallback ultime
					var parts = label.split(" ", 2);
					tradeName = parts[0];
					tradeId = (parts.length > 1) ? parts[1] : "";
				}

				Npcshopkeeper.debugLog(LOGGER, "Tentative de validation du trade : " + tradeName + " (ID: " + tradeId + ")");

				TradeHistory th = getTradeHistoryById(tradeId);
				boolean ongoing = (th != null && !th.isFinished());

				if (!ongoing) {
					LOGGER.warn("Le trade " + tradeId + " est déjà fini ou introuvable.");
					return;
				}

				// Validate item/req slots (0/1,2/3,...)
				if (isValidSlotPair(slots,0,1,player)
						&& isValidSlotPair(slots,2,3,player)
						&& isValidSlotPair(slots,4,5,player)
						&& isValidSlotPair(slots,6,7,player)) {

					Npcshopkeeper.debugLog(LOGGER, "Conditions remplies, exécution du trade...");

					clearAndRemoveSlots(player, slots);
					giveRewards(player, slots, tradeId, tradeName);
					markTradeAsFinished(player, tradeId);
					player.containerMenu.broadcastChanges();

					// ── SUPPRESSION DU PNJ À LA FIN DU TRADE ───────────────────────
					TradeHistory finishedTh = getTradeHistoryById(tradeId);
					if (finishedTh != null) {
						// … après markTradeAsFinished + broadcastChanges …

						String npcUuid = finishedTh.getNpcId();
						ServerLevel serverLevel = (ServerLevel) player.level();
						
						// Essayer de trouver l'entité par UUID
						Entity ent = null;
						try {
							ent = serverLevel.getEntity(UUID.fromString(npcUuid));
						} catch (Exception e) {
							LOGGER.warn("UUID invalide pour le PNJ : " + npcUuid);
						}
						
						if (ent instanceof TradeNpcEntity npcEnt) {
							for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
								// vérifie qu'il était bien sur cette route
								if (road.getNpcEntities().stream()
										.anyMatch(e -> e.getUUID().toString().equals(npcUuid))) {

									// 1) retire du JSON et mémoire forte
									road.removeNpcAndPersist(npcEnt);
									road.getNpcEntities().removeIf(e ->
											e.getUUID().equals(npcEnt.getUUID())
									);

									// 1.1) libère la place côté scheduler, PAR VALEUR
									var roadMap = NpcSpawnerManager.activeNPCs.get(road);
									if (roadMap != null) {
										roadMap.entrySet().removeIf(e ->
												e.getValue() instanceof TradeNpcEntity
														&& e.getValue().getUUID().equals(npcEnt.getUUID())
										);
									}
									break;
								}
							}

							// 2) despawn
							npcEnt.discard();

							// 3) supprime du JSON trades_npcs.json et recharge GlobalNpcManager
							JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
									Paths.get(OnServerStartedManager.PATH_NPCS),
									"npcs",
									TradeNpc::fromJson,
									TradeNpc::toJson
							);
							List<TradeNpc> kept = npcRepo.loadAll().stream()
									.filter(n -> !n.getNpcId().equals(npcUuid))
									.collect(Collectors.toList());
							npcRepo.saveAll(kept);
							
							// 4) Libérer le nom du PNJ pour qu'il puisse être réutilisé plus tard
							GlobalNpcManager.deactivateNpc(npcEnt.getNpcName());
							ActiveNpcManager.removeActiveNpc(UUID.fromString(npcUuid));

							Npcshopkeeper.debugLog(LOGGER, "🗑️ PNJ {} supprimé à la fin du trade", npcUuid);
						}
						else {
							LOGGER.warn("PNJ {} introuvable pour suppression", npcUuid);
						}
					}
				} else {
					Npcshopkeeper.debugLog(LOGGER, "Conditions non remplies pour le trade (items manquants ou slots de récompense pleins).");
				}
			}
		} catch (Exception e) {
			LOGGER.error("Erreur lors de l'exécution du trade", e);
		} finally {
			isProcessingTrade = false;
		}
	}
	private static boolean isServerPlayerWithMenu(Entity entity) {
		return entity instanceof ServerPlayer _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map;
	}

	// Méthode utilitaire pour récupérer les slots
	private static Map getSlots(ServerPlayer player) {
		return (Map) ((Supplier) player.containerMenu).get();
	}

	// Méthode utilitaire pour valider une paire de slots
	private static boolean isValidSlotPair(Map _slots, int slotId1, int slotId2, ServerPlayer player) {
		return (getItem(_slots, slotId1).getItem() == Items.AIR &&
				getItem(_slots, 8).getItem() == Items.AIR &&
				getItem(_slots, 9).getItem() == Items.AIR &&
				getItem(_slots, 10).getItem() == Items.AIR &&
				getItem(_slots, 11).getItem() == Items.AIR )
				||
				(getItem(_slots, slotId1).getItem() != Items.AIR &&
						getItem(_slots, slotId2).getItem() != Items.AIR &&
						getItem(_slots, 8).getItem() == Items.AIR &&
						getItem(_slots, 9).getItem() == Items.AIR &&
						getItem(_slots, 10).getItem() == Items.AIR &&
						getItem(_slots, 11).getItem() == Items.AIR &&
						getItem(_slots, slotId1).getItem() == getItem(_slots, slotId2).getItem() &&
						getAmount(_slots, slotId1) <= getAmount(_slots, slotId2));
	}

	// Méthode utilitaire pour effacer les slots et mettre à jour les quantités
	private static void clearAndRemoveSlots(ServerPlayer player, Map _slots) {
		removeItem(player, _slots, 0, 1);
		removeItem(player, _slots, 2, 3);
		removeItem(player, _slots, 4, 5);
		removeItem(player, _slots, 6, 7);
		clearSlot(player, _slots, 0);
		clearSlot(player, _slots, 2);
		clearSlot(player, _slots, 4);
		clearSlot(player, _slots, 6);
		player.containerMenu.broadcastChanges();
	}

	// Méthode utilitaire pour donner les récompenses
	private static void giveRewards(ServerPlayer player, Map _slots, String tradeId, String tradeName) {
		// Récupérer l'historique du trade
		Npcshopkeeper.debugLog(LOGGER, "On est dans give reward");
		TradeHistory tradeHistory = getTradeHistoryById(tradeId);
		if (tradeHistory == null) {
			LOGGER.error("Aucun historique de trade trouvé pour le trade ID : " + tradeId);
			return;
		}
		Npcshopkeeper.debugLog(LOGGER, "Historique de trade trouvé pour le joueur : " + tradeHistory.getPlayer());


		// Calculer le total d'argent à partir des tradeItems
		int totalMoneyInCopper = tradeHistory.getTotalPrice();
		Npcshopkeeper.debugLog(LOGGER, "Total d'argent calculé (en cuivre) : " + totalMoneyInCopper);

		// Convertir le total en pièces (Gold, Silver, Bronze, Copper)
		int[] coins = MoneyCalculator.getIntInCoins(totalMoneyInCopper);
		Npcshopkeeper.debugLog(LOGGER, "Conversion des pièces : Or = " + coins[0] + ", Argent = " + coins[1] + ", Bronze = " + coins[2] + ", Cuivre = " + coins[3]);

		// Définir les items correspondants aux pièces
		Item goldCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:gold_coin"));
		Item silverCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:silver_coin"));
		Item bronzeCoin = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:bronze_coin"));
		Item ironCoin   = BuiltInRegistries.ITEM.get(new ResourceLocation("medieval_coins:iron_coin"));

		// Tableau contenant les pièces et leur quantité
		ItemStack[] coinStacks = {
				new ItemStack(goldCoin, coins[0]),    // Or
				new ItemStack(silverCoin, coins[1]),  // Argent
				new ItemStack(bronzeCoin, coins[2]),  // Bronze
				new ItemStack(ironCoin, coins[3])   // Cuivre
		};

		// Trouver les deux types de pièces les plus hautes avec au moins une pièce
		int slotIndex = 8;
		for (int i = 0; i < coinStacks.length && slotIndex <= 9; i++) {
			if (coinStacks[i].getCount() > 0) {
				Npcshopkeeper.debugLog(LOGGER, "Ajout de " + coins[i] + " pièce(s) dans le slot " + slotIndex);
				setSlot(_slots, slotIndex, coinStacks[i], coins[i]);
				slotIndex++;
			}
		}

		// Récupérer le résultat du trade et placer l'item dans le slot 10
		TradeResult result = getTradeByName(tradeName).getResult();
		if (result == null) {
			LOGGER.error("Aucun résultat trouvé pour le trade : " + tradeName);
			return;
		}
		ResourceLocation itemResource = new ResourceLocation(result.getItem());
		Item item = BuiltInRegistries.ITEM.get(itemResource);
		Npcshopkeeper.debugLog(LOGGER, "Ajout de " + result.getQuantity() + " de " + result.getItem() + " dans le slot 10.");
		setSlot(_slots, 10, new ItemStack(item), result.getQuantity());

		// Si nécessaire, diffuser les changements de l'inventaire
		player.containerMenu.broadcastChanges();
		Npcshopkeeper.debugLog(LOGGER, "Changements de l'inventaire diffusés au joueur.");
	}



	// Méthode utilitaire pour effacer le contenu d'un slot
	private static void clearSlot(ServerPlayer player, Map _slots, int slotId) {
		((Slot) _slots.get(slotId)).set(ItemStack.EMPTY);
		player.containerMenu.broadcastChanges();
	}

	// Méthode utilitaire pour enlever les items d'un slot
	private static void removeItem(ServerPlayer player, Map _slots, int slotIdReq, int slotId) {
		ItemStack reqStack = ((Slot) _slots.get(slotIdReq)).getItem();
		ItemStack slotStack = ((Slot) _slots.get(slotId)).getItem();

		if (!slotStack.isEmpty() && !reqStack.isEmpty()) {
			int initialCount = slotStack.getCount();
			int reqCount = reqStack.getCount();
			int newCount = initialCount - reqCount;

			if (newCount < 0) newCount = 0;

			if (newCount > 0) {
				ItemStack updatedStack = slotStack.copy();
				updatedStack.setCount(newCount);
				((Slot) _slots.get(slotId)).set(updatedStack);
			} else {
				((Slot) _slots.get(slotId)).set(ItemStack.EMPTY);
			}

			player.containerMenu.broadcastChanges();
		}
	}

	private static void setSlot(Map _slots, int slotId, ItemStack stack, int count) {
		ItemStack _setstack = stack.copy();
		_setstack.setCount(count);
		((Slot) _slots.get(slotId)).set(_setstack);
	}

	private static ItemStack getItem(Map _slots, int slotId) {
		return _slots.containsKey(slotId) ? ((Slot) _slots.get(slotId)).getItem() : ItemStack.EMPTY;
	}

	private static int getAmount(Map _slots, int slotId) {
		ItemStack stack = getItem(_slots, slotId);
		return stack != null ? stack.getCount() : 0;
	}
}