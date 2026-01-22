package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.npc.ActiveNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import fr.renblood.npcshopkeeper.manager.trade.TradeSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CancelProcedure {
    private static final Logger LOGGER = LogManager.getLogger(CancelProcedure.class);

    public static void execute(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;

        try {
            Npcshopkeeper.debugLog(LOGGER, ">>> CancelProcedure START <<<");
            Npcshopkeeper.debugLog(LOGGER, "Container ID: " + player.containerMenu.containerId);

            // 1. Récupérer l'ID du trade depuis le TradeSessionManager
            String tradeId = TradeSessionManager.getTradeId(player.getUUID());
            
            if (tradeId == null || tradeId.isEmpty()) {
                LOGGER.warn("Impossible d'annuler : ID de trade introuvable dans la session.");
                // Fallback sur le slot 12 si la session est vide (pour compatibilité)
                if (player.containerMenu instanceof Supplier<?> sup && sup.get() instanceof Map<?, ?> rawSlots) {
                    @SuppressWarnings("unchecked")
                    Map<Integer, Slot> slots = (Map<Integer, Slot>) rawSlots;
                    ItemStack categoryStack = slots.get(12).getItem();
                    if (!categoryStack.isEmpty() && categoryStack.hasCustomHoverName()) {
                        String label = categoryStack.getHoverName().getString();
                        int lastSpaceIndex = label.lastIndexOf(' ');
                        if (lastSpaceIndex != -1) {
                            tradeId = label.substring(lastSpaceIndex + 1);
                        }
                    }
                }
            }
            
            if (tradeId == null || tradeId.isEmpty()) {
                LOGGER.warn("Echec total de récupération de l'ID du trade.");
                player.closeContainer();
                return;
            }

            // 2. Marquer le trade comme fini (annulé) dans l'historique
            JsonRepository<TradeHistory> historyRepo = new JsonRepository<>(
                    Paths.get(OnServerStartedManager.PATH_HISTORY), // Utilisation de OnServerStartedManager
                    "history",
                    TradeHistory::fromJson,
                    TradeHistory::toJson
            );
            List<TradeHistory> allHistories = new ArrayList<>(historyRepo.loadAll());
            TradeHistory targetHistory = null;
            
            for (TradeHistory h : allHistories) {
                if (h.getId().equals(tradeId)) {
                    h.setFinished(true);
                    h.setAbandoned(true); // Marquer comme abandonné
                    targetHistory = h;
                    break;
                }
            }
            
            if (targetHistory != null) {
                historyRepo.saveAll(allHistories);
                LOGGER.info("Trade {} annulé et marqué comme abandonné par le joueur.", tradeId);
                
                // Nettoyage de la session
                TradeSessionManager.clearTradeId(player.getUUID());

                // 3. Supprimer le PNJ associé
                String npcUuid = targetHistory.getNpcId();
                if (npcUuid != null && !npcUuid.isEmpty()) {
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    
                    // Essayer de trouver l'entité par UUID
                    Entity ent = null;
                    try {
                        ent = serverLevel.getEntity(UUID.fromString(npcUuid));
                    } catch (Exception e) {
                        LOGGER.warn("UUID invalide pour le PNJ : " + npcUuid);
                    }
                    
                    if (ent instanceof TradeNpcEntity npcEnt) {
                        // Nettoyage des routes
                        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
                            if (road.getNpcEntities().stream()
                                    .anyMatch(e -> e.getUUID().toString().equals(npcUuid))) {

                                road.removeNpcAndPersist(npcEnt);
                                road.getNpcEntities().removeIf(e -> e.getUUID().equals(npcEnt.getUUID()));

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

                        // Despawn
                        npcEnt.discard();

                        // Nettoyage JSON trades_npcs.json
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
                        
                        // Libérer le nom
                        GlobalNpcManager.deactivateNpc(npcEnt.getNpcName());
                        ActiveNpcManager.removeActiveNpc(UUID.fromString(npcUuid));

                        LOGGER.info("🗑️ PNJ {} supprimé suite à l'annulation du trade", npcUuid);
                        player.displayClientMessage(Component.literal("Vous avez refusé l'offre. Le marchand s'en va."), true);
                    }
                }
            }

            // 4. Fermer l'interface
            player.closeContainer();

        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'annulation du trade", e);
            player.closeContainer();
        }
    }
}
