package fr.renblood.npcshopkeeper.data.api;

import java.util.List;
import java.util.Map;

public class NpcTemplate {
    public String npc_id;
    public String name;
    public String type; // DECO, SHOPKEEPER, QUEST
    public String texture;
    public List<String> dialogue;
    public List<String> tags;
    public List<QuestLink> quest_links;
    public Map<String, Object> implementation;
    public boolean enabled;
    
    // Champs spécifiques (simplifiés pour l'instant, on pourra utiliser des maps ou sous-objets)
    public String shop_id;
    public String trade_category;
    
    @Override
    public String toString() {
        return "NpcTemplate{id='" + npc_id + "', name='" + name + "', type='" + type + "'}";
    }
}
