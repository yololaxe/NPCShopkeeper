package fr.renblood.npcshopkeeper.manager.data;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DataValidator {
    private DataValidator() {}

    public static List<String> validate() {
        List<String> issues = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<String> categories = new HashSet<>();
        RuntimeDataCache.getTrades().forEach(trade -> {
            if (trade.getCategory() != null) categories.add(trade.getCategory().toLowerCase());
        });
        List<TradeNpc> rawNpcs = new JsonRepository<>(Paths.get(OnServerStartedManager.PATH_NPCS), "npcs",
                TradeNpc::fromJson, TradeNpc::toJson).loadAll();
        for (TradeNpc npc : rawNpcs) {
            if (npc.getNpcId() == null || !ids.add(npc.getNpcId())) issues.add("UUID PNJ duplique/invalide: " + npc.getNpcId());
            if (npc.getPos() == null || npc.getPos().getY() < -64 || npc.getPos().getY() > 320) issues.add("Position PNJ invalide: " + npc.getNpcId());
            if (GlobalNpcManager.getNpcData(npc.getNpcName()) == null) issues.add("Modele PNJ inexistant: " + npc.getNpcName());
            if (npc.getTradeCategory() != null && !categories.contains(npc.getTradeCategory().toLowerCase())) {
                issues.add("Categorie inconnue: " + npc.getTradeCategory() + " (" + npc.getNpcId() + ")");
            }
        }
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            if (road.getPositions() == null || road.getPositions().isEmpty()) issues.add("Route sans point: " + road.getName());
        }
        NpcEntityIndex.all().forEach(entity -> {
            String apiNpcId = entity.getPersistentData().getString("ApiNpcId");
            if (!apiNpcId.isBlank() && GlobalNpcManager.getNpcDataById(apiNpcId) == null) {
                issues.add("Spawn API lie a un PNJ supprime: " + entity.getApiNpcSpawnId());
            }
        });
        return issues;
    }
}
