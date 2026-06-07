package fr.renblood.npcshopkeeper.manager.integration;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.data.api.NpcTemplate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MedievalCoinsIntegration {
    private static final Logger LOGGER = LogManager.getLogger(MedievalCoinsIntegration.class);
    private static boolean isMedievalCoinsLoaded = false;
    private static Method addJobXpMethod;
    private static Method getNpcsMethod;
    private static Method getNpcSpawnsMethod;
    private static Method getNpcSpawnsByWorldMethod;
    private static Method createNpcMethod;
    private static Method createNpcSpawnMethod;
    
    private static Class<?> npcModelClass;
    private static Class<?> npcSpawnModelClass;

    static {
        LOGGER.info("Tentative de chargement de l'intégration MedievalCoins...");
        
        if (ModList.get().isLoaded("medieval_coins")) {
            LOGGER.info("Le mod 'medieval_coins' est bien détecté par Forge.");
        } else {
            LOGGER.warn("Le mod 'medieval_coins' N'EST PAS détecté par Forge !");
        }

        try {
            String apiClassName = "fr.renblood.medievalcoins.api.MedievalCoinsAPI";
            Class<?> apiClass = Class.forName(apiClassName);
            LOGGER.info("Classe API chargée : " + apiClass.getName());

            try {
                npcModelClass = Class.forName("fr.renblood.medievalcoins.api.model.NpcModel");
                npcSpawnModelClass = Class.forName("fr.renblood.medievalcoins.api.model.NpcSpawnModel");
                LOGGER.info("Classes modèles trouvées.");
            } catch (Throwable e) {
                LOGGER.error("Erreur chargement modèles : " + e.toString());
            }

            try {
                addJobXpMethod = apiClass.getMethod("addJobXp", ServerPlayer.class, String.class, int.class);
                getNpcsMethod = apiClass.getMethod("getNpcs");
                getNpcSpawnsMethod = apiClass.getMethod("getNpcSpawns");
                try {
                    getNpcSpawnsByWorldMethod = apiClass.getMethod("getNpcSpawns", String.class, boolean.class);
                } catch (NoSuchMethodException e) {
                    LOGGER.warn("getNpcSpawns(String, boolean) indisponible, utilisation du fallback global.");
                }
                
                if (npcModelClass != null) {
                    createNpcMethod = apiClass.getMethod("createNpc", npcModelClass);
                }
                if (npcSpawnModelClass != null) {
                    createNpcSpawnMethod = apiClass.getMethod("createNpcSpawn", npcSpawnModelClass);
                }
                
                isMedievalCoinsLoaded = true;
                LOGGER.info("MedievalCoins API détectée et chargée avec succès.");
            } catch (NoSuchMethodException e) {
                LOGGER.error("Méthode manquante dans l'API : " + e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Classe API non trouvée (ClassNotFoundException) : " + e.getMessage());
        } catch (NoClassDefFoundError e) {
            LOGGER.error("Classe API trouvée mais dépendance manquante (NoClassDefFoundError) : " + e.getMessage());
        } catch (Throwable e) {
            LOGGER.error("Erreur critique lors du chargement de l'API : " + e.toString(), e);
        }
    }

    public static void addXp(ServerPlayer player, String job, float amount) {
        if (!isMedievalCoinsLoaded || addJobXpMethod == null) return;
        try {
            int intAmount = Math.round(amount);
            if (intAmount > 0) {
                addJobXpMethod.invoke(null, player, job, intAmount);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur addJobXp", e);
        }
    }
    
    public static boolean createNpc(NpcTemplate template) {
        if (!isMedievalCoinsLoaded || createNpcMethod == null) return false;
        try {
            Object model = npcModelClass.getDeclaredConstructor().newInstance();
            setField(model, "name", template.name);
            setField(model, "type", template.type);
            setField(model, "texture", template.texture); // skin -> texture
            setField(model, "dialogue", template.dialogue);
            setField(model, "tags", template.tags);
            setField(model, "enabled", template.enabled);
            setField(model, "tradeCategory", template.trade_category); // trade_category -> tradeCategory
            
            // Ajout du npcId s'il est défini
            if (template.npc_id != null) setField(model, "npcId", template.npc_id); // npc_id -> npcId
            
            return (boolean) createNpcMethod.invoke(null, model);
        } catch (Exception e) {
            LOGGER.error("Erreur createNpc", e);
            return false;
        }
    }
    
    public static boolean createNpcSpawn(NpcSpawn spawn) {
        if (!isMedievalCoinsLoaded || createNpcSpawnMethod == null) return false;
        try {
            Object model = npcSpawnModelClass.getDeclaredConstructor().newInstance();
            setField(model, "spawnId", spawn.spawn_id); // spawn_id -> spawnId
            setField(model, "npcId", spawn.npc_id); // npc_id -> npcId
            setField(model, "world", spawn.world);
            setField(model, "x", (double)spawn.x);
            setField(model, "y", (double)spawn.y);
            setField(model, "z", (double)spawn.z);
            setField(model, "yaw", spawn.yaw);
            setField(model, "pitch", spawn.pitch);
            setField(model, "spawnRule", spawn.spawn_rule); // spawn_rule -> spawnRule
            setField(model, "active", spawn.active);
            setField(model, "meta", spawn.meta);
            
            return (boolean) createNpcSpawnMethod.invoke(null, model);
        } catch (Exception e) {
            LOGGER.error("Erreur createNpcSpawn", e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<NpcTemplate> getNpcs() {
        List<NpcTemplate> result = new ArrayList<>();
        if (!isMedievalCoinsLoaded || getNpcsMethod == null) return result;

        try {
            List<?> apiList = (List<?>) getNpcsMethod.invoke(null);
            if (apiList == null) return result;

            for (Object obj : apiList) {
                NpcTemplate template = new NpcTemplate();
                Class<?> clazz = obj.getClass();
                
                template.npc_id = getStringField(obj, clazz, "npcId"); // npc_id -> npcId
                template.name = getStringField(obj, clazz, "name");
                template.type = getStringField(obj, clazz, "type");
                template.texture = getStringField(obj, clazz, "texture"); // skin -> texture
                template.dialogue = (List<String>) getField(obj, clazz, "dialogue");
                template.tags = (List<String>) getField(obj, clazz, "tags");
                template.enabled = getBooleanField(obj, clazz, "enabled");
                template.trade_category = getStringField(obj, clazz, "tradeCategory"); // trade_category -> tradeCategory
                
                result.add(template);
            }
            Npcshopkeeper.debugLog(LOGGER, "Récupéré {} NpcTemplates depuis l'API.", result.size());
        } catch (Exception e) {
            LOGGER.error("Erreur getNpcs", e);
        }
        return result;
    }

    public static List<NpcSpawn> getNpcSpawns() {
        List<NpcSpawn> result = new ArrayList<>();
        if (!isMedievalCoinsLoaded || getNpcSpawnsMethod == null) return result;

        try {
            List<?> apiList = (List<?>) getNpcSpawnsMethod.invoke(null);
            if (apiList == null) return result;

            for (Object obj : apiList) {
                NpcSpawn spawn = new NpcSpawn();
                Class<?> clazz = obj.getClass();

                spawn.spawn_id = getStringField(obj, clazz, "spawnId"); // spawn_id -> spawnId
                spawn.npc_id = getStringField(obj, clazz, "npcId"); // npc_id -> npcId
                spawn.world = getStringField(obj, clazz, "world");
                spawn.x = getDoubleField(obj, clazz, "x");
                spawn.y = getDoubleField(obj, clazz, "y");
                spawn.z = getDoubleField(obj, clazz, "z");
                spawn.yaw = (float) getDoubleField(obj, clazz, "yaw");
                spawn.pitch = (float) getDoubleField(obj, clazz, "pitch");
                spawn.spawn_rule = getStringField(obj, clazz, "spawnRule"); // spawn_rule -> spawnRule
                spawn.active = getBooleanField(obj, clazz, "active");
                spawn.npc_name = getStringField(obj, clazz, "npcName");
                spawn.npc_type = getStringField(obj, clazz, "npcType");
                spawn.npc_skin = getStringField(obj, clazz, "npcSkin");
                spawn.dialogue = (List<String>) getField(obj, clazz, "dialogue");
                spawn.quest_ids = (List<String>) getField(obj, clazz, "questIds");
                spawn.meta = (Map<String, Object>) getField(obj, clazz, "meta");

                result.add(spawn);
            }
            Npcshopkeeper.debugLog(LOGGER, "Récupéré {} NpcSpawns depuis l'API.", result.size());
        } catch (Exception e) {
            LOGGER.error("Erreur getNpcSpawns", e);
        }
        return result;
    }

    public static List<NpcSpawn> getNpcSpawns(String world) {
        if (isMedievalCoinsLoaded && getNpcSpawnsByWorldMethod != null) {
            try {
                List<?> apiList = (List<?>) getNpcSpawnsByWorldMethod.invoke(null, world, true);
                return convertNpcSpawns(apiList);
            } catch (Exception e) {
                LOGGER.error("Erreur getNpcSpawns({}, true), utilisation du fallback global.", world, e);
            }
        }
        return getNpcSpawns().stream()
                .filter(spawn -> world != null && world.equals(spawn.world))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static List<NpcSpawn> convertNpcSpawns(List<?> apiList) {
        List<NpcSpawn> result = new ArrayList<>();
        if (apiList == null) return result;

        for (Object obj : apiList) {
            NpcSpawn spawn = new NpcSpawn();
            Class<?> clazz = obj.getClass();
            spawn.spawn_id = getStringField(obj, clazz, "spawnId");
            spawn.npc_id = getStringField(obj, clazz, "npcId");
            spawn.world = getStringField(obj, clazz, "world");
            spawn.x = getDoubleField(obj, clazz, "x");
            spawn.y = getDoubleField(obj, clazz, "y");
            spawn.z = getDoubleField(obj, clazz, "z");
            spawn.yaw = (float) getDoubleField(obj, clazz, "yaw");
            spawn.pitch = (float) getDoubleField(obj, clazz, "pitch");
            spawn.spawn_rule = getStringField(obj, clazz, "spawnRule");
            spawn.active = getBooleanField(obj, clazz, "active");
            spawn.npc_name = getStringField(obj, clazz, "npcName");
            spawn.npc_type = getStringField(obj, clazz, "npcType");
            spawn.npc_skin = getStringField(obj, clazz, "npcSkin");
            spawn.dialogue = (List<String>) getField(obj, clazz, "dialogue");
            spawn.quest_ids = (List<String>) getField(obj, clazz, "questIds");
            spawn.meta = (Map<String, Object>) getField(obj, clazz, "meta");
            result.add(spawn);
        }
        return result;
    }

    // Helpers de réflexion
    private static void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (NoSuchFieldException e) {
            LOGGER.error("Champ manquant dans le modèle MedievalCoins : " + fieldName + " (" + obj.getClass().getName() + ")");
        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'écriture du champ " + fieldName, e);
        }
    }

    private static Object getField(Object obj, Class<?> clazz, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStringField(Object obj, Class<?> clazz, String fieldName) {
        Object val = getField(obj, clazz, fieldName);
        return val != null ? val.toString() : "";
    }

    private static int getIntField(Object obj, Class<?> clazz, String fieldName) {
        Object val = getField(obj, clazz, fieldName);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private static double getDoubleField(Object obj, Class<?> clazz, String fieldName) {
        Object val = getField(obj, clazz, fieldName);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0D;
    }

    private static boolean getBooleanField(Object obj, Class<?> clazz, String fieldName) {
        Object val = getField(obj, clazz, fieldName);
        return val instanceof Boolean ? (Boolean) val : false;
    }
}
