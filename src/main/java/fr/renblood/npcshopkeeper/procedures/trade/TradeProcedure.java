package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.price.TradeItemInfo;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.trade.TradeResult;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import fr.renblood.npcshopkeeper.manager.trade.TradeSessionManager;
import fr.renblood.npcshopkeeper.manager.trade.XpReferenceManager;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TradeProcedure {
	private static final Logger LOGGER = LogManager.getLogger(TradeProcedure.class);

	// ── Helpers ─────────────────────────────────────────────────────────

    private static Trade getTradeByName(String name) {
        return fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache.getTrade(name);
    }
    private static TradeHistory getTradeHistoryById(String id) {
        return fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache.getHistory(id);
    }

	private static void markTradeAsFinished(ServerPlayer player, String id) {
		JsonRepository<TradeHistory> repo = new JsonRepository<>(
				Paths.get(OnServerStartedManager.PATH_HISTORY),
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
        if (!(entity instanceof ServerPlayer actor)
                || !(actor.containerMenu instanceof fr.renblood.npcshopkeeper.world.inventory.TradeMenu requestedMenu)) return;
        if (requestedMenu.isSettlementPending()) {
            actor.displayClientMessage(Component.literal("Un règlement est déjà en cours pour ce contrat.")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
            return;
        }

		try {
			if (entity instanceof ServerPlayer player
					&& player.containerMenu instanceof Supplier<?> sup
					&& sup.get() instanceof Map<?, ?> rawSlots) {

				Map<Integer, Slot> slots = (Map<Integer, Slot>) rawSlots;

				// Récupération de l'ID du trade depuis le TradeSessionManager
				String tradeId = TradeSessionManager.getTradeId(player.getUUID());
				
				if (tradeId == null || tradeId.isEmpty()) {
					LOGGER.warn("Impossible de valider : ID de trade introuvable dans la session.");
					// Fallback sur le slot 12 si la session est vide (pour compatibilité)
					ItemStack categoryStack = slots.get(12).getItem();
					if (!categoryStack.isEmpty() && categoryStack.hasCustomHoverName()) {
						String label = categoryStack.getHoverName().getString();
						int lastSpaceIndex = label.lastIndexOf(' ');
						if (lastSpaceIndex != -1) {
							tradeId = label.substring(lastSpaceIndex + 1);
						}
					}
				}
				
				if (tradeId == null || tradeId.isEmpty()) {
					LOGGER.warn("Echec total de récupération de l'ID du trade.");
					return;
				}

				Npcshopkeeper.debugLog(LOGGER, "Tentative de validation du trade (ID: " + tradeId + ")");

				TradeHistory th = getTradeHistoryById(tradeId);
				boolean ongoing = (th != null && !th.isFinished());

				if (!ongoing) {
					LOGGER.warn("Le trade " + tradeId + " est déjà fini ou introuvable.");
					return;
				}
				
				String tradeName = th.getTradeName();

				// Validate item/req slots (0/1,2/3,...)
				if (isValidSlotPair(slots,0,1,player)
						&& isValidSlotPair(slots,2,3,player)
						&& isValidSlotPair(slots,4,5,player)
						&& isValidSlotPair(slots,6,7,player)) {

					Npcshopkeeper.debugLog(LOGGER, "Conditions remplies, exécution du trade...");

                    final String resolvedTradeId=tradeId;
                    final var expectedMenu=player.containerMenu;
                    long amount=th.getTotalPrice();
                    if(amount<=0){player.sendSystemMessage(Component.literal("Offre sans prix confirmé : contactez un administrateur."));return;}
                    Trade definition = getTradeByName(tradeName);
                    if (definition == null) return;
                    TradeResult bonus = definition.getResult();
                    ItemStack bonusStack = ItemStack.EMPTY;
                    if (bonus != null && bonus.getQuantity() > 0) {
                        ResourceLocation bonusId = ResourceLocation.tryParse(bonus.getItem());
                        if (bonusId == null || !BuiltInRegistries.ITEM.containsKey(bonusId)) return;
                        bonusStack = new ItemStack(BuiltInRegistries.ITEM.get(bonusId), bonus.getQuantity());
                    }
                    requestedMenu.beginSettlement();
                    fr.renblood.npcshopkeeper.manager.integration.CommerceAdapter.cashContract(player,
                        "npc-trade:" + resolvedTradeId, "contract:" + resolvedTradeId, amount, bonusStack, () -> {
                            TradeHistory current = getTradeHistoryById(resolvedTradeId);
                            return player.containerMenu == expectedMenu && current != null && !current.isFinished()
                                    && isValidSlotPair(slots, 0, 1, player) && isValidSlotPair(slots, 2, 3, player)
                                    && isValidSlotPair(slots, 4, 5, player) && isValidSlotPair(slots, 6, 7, player);
                        }, () -> {
					clearAndRemoveSlots(player, slots);
					giveTradeExperience(player, resolvedTradeId);
					markTradeAsFinished(player, resolvedTradeId);
					
					// Nettoyage de la session
					TradeSessionManager.clearTradeId(player.getUUID());

					player.containerMenu.broadcastChanges();

					// ── SUPPRESSION DU PNJ À LA FIN DU TRADE ───────────────────────
					TradeHistory finishedTh = getTradeHistoryById(resolvedTradeId);
					if (finishedTh != null) {
						// … après markTradeAsFinished + broadcastChanges …

						String npcUuid = finishedTh.getNpcId();
						ServerLevel serverLevel = (ServerLevel) player.level();
						
						// Recherche plus robuste du PNJ : d'abord par UUID direct, puis par proximité
						Entity ent = serverLevel.getEntity(UUID.fromString(npcUuid));
						
						if (ent == null) {
							// Fallback : chercher autour du joueur
							AABB searchBox = player.getBoundingBox().inflate(10);
							List<TradeNpcEntity> nearbyNpcs = serverLevel.getEntitiesOfClass(TradeNpcEntity.class, searchBox);
							for (TradeNpcEntity nearby : nearbyNpcs) {
								if (nearby.getUUID().toString().equals(npcUuid)) {
									ent = nearby;
									break;
								}
							}
						}
						
						if (ent instanceof TradeNpcEntity npcEnt) {
							// Vérifier si c'est un PNJ de route (qui doit disparaître)
							// Les PNJs fixes (non route) ne doivent pas disparaître
							// On vérifie s'il appartient à une route active
							boolean isRouteNpc = false;
							
							for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
								// vérifie qu'il était bien sur cette route
								if (road.getNpcEntities().stream()
										.anyMatch(e -> e.getUUID().toString().equals(npcUuid))) {
									isRouteNpc = true;

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
							
							// Si ce n'est pas un PNJ de route, on vérifie s'il est marqué comme tel dans son modèle
							if (!isRouteNpc && npcEnt.getTradeNpc() != null && npcEnt.getTradeNpc().isRouteNpc()) {
								isRouteNpc = true;
							}

							if (isRouteNpc) {
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
							} else {
								Npcshopkeeper.debugLog(LOGGER, "PNJ {} est fixe, il ne disparaît pas.", npcUuid);
							}
						}
						else {
							LOGGER.warn("PNJ {} introuvable pour suppression (même après recherche locale)", npcUuid);
						}
					}
                        }, fr.renblood.npcshopkeeper.services.ContractService.terms(th.getNpcId(),"contract:"+resolvedTradeId,th)).whenComplete((receipt,failure)->player.getServer().execute(()->{
                            requestedMenu.endSettlement();
                            if (player.hasDisconnected()) return;
                            if (failure != null) {
                                Throwable cause = failure;
                                while (cause.getCause() != null) cause = cause.getCause();
                                String message = String.valueOf(cause.getMessage()).contains("INSUFFICIENT_FUNDS")
                                        ? "Le budget du marchand est insuffisant. Contacte l’équipe pour vérifier son financement."
                                        : "Échange interrompu. Garde le reçu et vérifie son état avant de réessayer.";
                                player.sendSystemMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED));
                            } else if (receipt != null && "CONFIRMED".equals(receipt.get("state").getAsString())) {
                                // Coins displays the confirmed item/coin breakdown once, with the receipt on hover.
                            } else if (receipt != null && "CANCELLED".equals(receipt.get("state").getAsString())) {
                                player.sendSystemMessage(Component.literal("Échange annulé. Vérifie les objets déposés et la place pour recevoir les pièces.")
                                        .withStyle(net.minecraft.ChatFormatting.RED));
                            } else {
                                player.sendSystemMessage(Component.literal("Ce règlement doit être vérifié. Transmets la référence du reçu à l’équipe.")
                                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
                            }
                        }));
				} else {
					Npcshopkeeper.debugLog(LOGGER, "Conditions non remplies pour le trade (items manquants ou slots de récompense pleins).");
				}
			}
		} catch (Exception e) {
            requestedMenu.endSettlement();
			LOGGER.error("Erreur lors de l'exécution du trade", e);
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
						ItemStack.isSameItemSameTags(getItem(_slots, slotId1), getItem(_slots, slotId2)) &&
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
	private static void giveTradeExperience(ServerPlayer player, String tradeId) {
		// Récupérer l'historique du trade
		Npcshopkeeper.debugLog(LOGGER, "On est dans give reward");
		TradeHistory tradeHistory = getTradeHistoryById(tradeId);
		if (tradeHistory == null) {
			LOGGER.error("Aucun historique de trade trouvé pour le trade ID : " + tradeId);
			return;
		}
		Npcshopkeeper.debugLog(LOGGER, "Historique de trade trouvé pour le joueur : " + tradeHistory.getPlayer());


		// ── CALCUL ET DISTRIBUTION DE L'XP ───────────────────────────────────
		Map<String, Float> xpGains = new HashMap<>(); // Job -> Total XP
		
		for (TradeItemInfo info : tradeHistory.getTradeItems()) {
			XpReferenceManager.XpInfo xpRef = XpReferenceManager.getXpReference(info.getItem());
			if (xpRef != null) {
				String job = xpRef.job;
				float xpPerItem = xpRef.getRandomXp(); // Utilisation de la valeur aléatoire entre min et max
				float totalXp = xpPerItem * info.getQuantity();
				
				xpGains.put(job, xpGains.getOrDefault(job, 0f) + totalXp);
				
				// Log de vérification
				Npcshopkeeper.debugLog(LOGGER, "XP calculée pour " + info.getItem() + " : " + totalXp + " (" + job + ")");
			} else {
				// Log si pas de ref
				// player.displayClientMessage(Component.literal("Pas de ref XP pour " + info.getItem()), false);
			}
		}
		
		for (Map.Entry<String, Float> entry : xpGains.entrySet()) {
			MedievalCoinsIntegration.addXp(player, entry.getKey(), entry.getValue());
			Npcshopkeeper.debugLog(LOGGER, "Envoi XP à MedievalCoins : " + entry.getKey() + " -> " + entry.getValue());
		}
		// ─────────────────────────────────────────────────────────────────────

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