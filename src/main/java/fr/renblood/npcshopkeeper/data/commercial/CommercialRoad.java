package fr.renblood.npcshopkeeper.data.commercial;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.UUID;

import static com.mojang.text2speech.Narrator.LOGGER;

public class CommercialRoad {
    private String id;
    private String name;
    private String category;
    private ArrayList<BlockPos> positions;
    private ArrayList<TradeNpcEntity> npcEntities;
    private int minTimer;
    private int maxTimer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ArrayList<BlockPos> getPositions() {
        return positions;
    }

    public void setPositions(ArrayList<BlockPos> positions) {
        this.positions = positions;
    }

    public ArrayList<TradeNpcEntity> getNpcEntities() {
        return npcEntities;
    }

    public void setNpcEntities(ArrayList<TradeNpcEntity> npcEntities) {
        this.npcEntities = npcEntities;
    }

    public int getMinTimer() {
        return minTimer;
    }

    public void setMinTimer(int minTimer) {
        this.minTimer = minTimer;
    }

    public int getMaxTimer() {
        return maxTimer;
    }

    public void setMaxTimer(int maxTimer) {
        this.maxTimer = maxTimer;
    }
    public void removeNpcEntity(TradeNpcEntity npc) {
        this.npcEntities.remove(npc);
    }

    public CommercialRoad(String id, String name, String category, ArrayList<BlockPos> positions, ArrayList<TradeNpcEntity> npcEntities, int minTimer, int maxTimer) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.positions = positions;
        this.npcEntities = npcEntities;
        this.minTimer = minTimer;
        this.maxTimer = maxTimer;
    }

    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }


    public static void updateCommercialRoadAfterRemoval(TradeNpcEntity npc) {
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            if (road.getNpcEntities().contains(npc)) {
                road.removeNpcEntity(npc);
                LOGGER.info("NPC supprimé de la route commerciale : " + road.getName());
                return;
            }
        }
    }



}