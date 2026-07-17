package fr.renblood.npcshopkeeper.manager.integration;

import com.google.gson.JsonObject;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.data.api.NpcTemplate;
import fr.renblood.npcshopkeeper.data.api.QuestLink;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MedievalCoinsIntegration {
    private static final Logger LOGGER = LogManager.getLogger(MedievalCoinsIntegration.class);
    private static boolean isMedievalCoinsLoaded = false;
    private static Method addJobXpMethod;
    private static Method getNpcsMethod;
    private static Method getNpcSpawnsMethod;
    private static Method getNpcSpawnsByWorldMethod;
    private static Method getNpcChangesSinceMethod;
    private static Method getNpcRevisionMethod;
    private static Method createNpcMethod;
    private static Method createNpcSpawnMethod;
    private static Method deleteNpcSpawnMethod;
    private static Method deactivateNpcSpawnMethod;
    private static Method completeQuestFromNpcMethod;
    private static Method showNpcDialogueMethod;
    private static Method openNpcQuestInteractionsMethod;
    private static Method getQuestDetailsMethod;
    private static Method getActiveQuestsMethod;
    private static Method completeQuestMethod;
    private static Method getPlayerMethod;
    private static Method getReferencePriceMethod;
    private static Method getReferenceXpJobItemMethod;
    private static Method getReferenceXpActionJobItemMethod;
    private static Method loadConfigMethod;
    private static Method sendPostRequestMethod;
    private static Class<?> configClass;
    
    private static Class<?> npcModelClass;
    private static Class<?> npcSpawnModelClass;

    public static boolean isLoaded() {
        return isMedievalCoinsLoaded;
    }

    public static boolean supportsDifferentialSync() {
        return isMedievalCoinsLoaded && getNpcChangesSinceMethod != null && getNpcRevisionMethod != null;
    }

    public static boolean hasNpcChangesSince(long revision) {
        if (!supportsDifferentialSync()) return true;
        try {
            Object changes = getNpcChangesSinceMethod.invoke(null, revision);
            if (changes instanceof java.util.Collection<?> collection) return !collection.isEmpty();
            if (changes instanceof Boolean changed) return changed;
            return changes != null;
        } catch (Exception e) {
            LOGGER.error("Erreur getNpcChangesSince({})", revision, e);
            return false;
        }
    }

    public static long getNpcRevision() {
        if (!supportsDifferentialSync()) return 0L;
        try {
            Object revision = getNpcRevisionMethod.invoke(null);
            return revision instanceof Number number ? number.longValue() : 0L;
        } catch (Exception e) {
            LOGGER.error("Erreur getNpcRevision()", e);
            return 0L;
        }
    }

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
                completeQuestFromNpcMethod = apiClass.getMethod("completeQuestFromNpc", ServerPlayer.class, String.class);
                showNpcDialogueMethod = apiClass.getMethod(
                        "showNpcDialogue", ServerPlayer.class, String.class, String.class, String.class);
                openNpcQuestInteractionsMethod = apiClass.getMethod(
                        "openNpcQuestInteractions", ServerPlayer.class, String.class, String.class, String.class);
                getNpcsMethod = apiClass.getMethod("getNpcs");
                getNpcSpawnsMethod = apiClass.getMethod("getNpcSpawns");
                try {
                    getNpcSpawnsByWorldMethod = apiClass.getMethod("getNpcSpawns", String.class, boolean.class);
                } catch (NoSuchMethodException e) {
                    LOGGER.warn("getNpcSpawns(String, boolean) indisponible, utilisation du fallback global.");
                }
                try {
                    getNpcChangesSinceMethod = apiClass.getMethod("getNpcChangesSince", long.class);
                    getNpcRevisionMethod = apiClass.getMethod("getNpcRevision");
                } catch (NoSuchMethodException e) {
                    LOGGER.info("Synchronisation differentielle Medieval Coins indisponible.");
                }
                
                if (npcModelClass != null) {
                    createNpcMethod = apiClass.getMethod("createNpc", npcModelClass);
                }
                if (npcSpawnModelClass != null) {
                    createNpcSpawnMethod = apiClass.getMethod("createNpcSpawn", npcSpawnModelClass);
                }
                try {
                    deleteNpcSpawnMethod = apiClass.getMethod("deleteNpcSpawn", String.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    deactivateNpcSpawnMethod = apiClass.getMethod("deactivateNpcSpawn", String.class);
                } catch (NoSuchMethodException ignored) {
                }

                Class<?> apiClientClass = Class.forName("fr.renblood.medievalcoins.network.ApiClient");
                getQuestDetailsMethod = apiClientClass.getMethod("getQuestDetails", String.class);
                getActiveQuestsMethod = apiClientClass.getMethod("getActiveQuests", String.class, String.class);
                completeQuestMethod = apiClientClass.getMethod("completeQuest", String.class, String.class);
                getPlayerMethod = apiClientClass.getMethod("getPlayer", String.class);
                sendPostRequestMethod = apiClientClass.getDeclaredMethod(
                        "sendPostRequest", String.class, String.class, JsonObject.class);
                sendPostRequestMethod.setAccessible(true);
                configClass = Class.forName("fr.renblood.medievalcoins.config.ModConfig");
                loadConfigMethod = configClass.getMethod("load");

                try {
                    getReferencePriceMethod = apiClass.getMethod("getReferencePrice", String.class);
                    getReferenceXpJobItemMethod = apiClass.getMethod("getReferenceXp", String.class, String.class);
                    getReferenceXpActionJobItemMethod = apiClass.getMethod(
                            "getReferenceXp", String.class, String.class, String.class);
                    LOGGER.info("API references MedievalCoins disponible.");
                } catch (NoSuchMethodException e) {
                    LOGGER.info("API references MedievalCoins indisponible, fallback JSON local.");
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

    public static Optional<Integer> getReferencePrice(String itemId) {
        if (!isMedievalCoinsLoaded || getReferencePriceMethod == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        try {
            return extractInteger(getReferencePriceMethod.invoke(null, itemId),
                    "referencePrice", "calculatedPrice", "buyPrice", "sellPrice", "price");
        } catch (Exception e) {
            LOGGER.error("Erreur getReferencePrice({})", itemId, e);
            return Optional.empty();
        }
    }

    public static Optional<XpReferenceInfo> getReferenceXp(String job, String itemId) {
        if (!isMedievalCoinsLoaded || getReferenceXpJobItemMethod == null || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }
        try {
            return extractXpReference(getReferenceXpJobItemMethod.invoke(null, job, itemId), job);
        } catch (Exception e) {
            LOGGER.error("Erreur getReferenceXp({}, {})", job, itemId, e);
            return Optional.empty();
        }
    }

    public static Optional<XpReferenceInfo> getReferenceXp(String action, String job, String itemId) {
        if (!isMedievalCoinsLoaded || getReferenceXpActionJobItemMethod == null || itemId == null || itemId.isBlank()) {
            return getReferenceXp(job, itemId);
        }
        try {
            return extractXpReference(getReferenceXpActionJobItemMethod.invoke(null, action, job, itemId), job);
        } catch (Exception e) {
            LOGGER.error("Erreur getReferenceXp({}, {}, {})", action, job, itemId, e);
            return Optional.empty();
        }
    }
    
    public static boolean createNpc(NpcTemplate template) {
        if (!isMedievalCoinsLoaded || createNpcMethod == null) return false;
        try {
            Object model = npcModelClass.getDeclaredConstructor().newInstance();
            setField(model, "name", template.name);
            setField(model, "type", template.type);
            setField(model, "texture", template.texture); // skin -> texture
            setField(model, "dialogue", template.dialogue != null ? template.dialogue : new ArrayList<>());
            setField(model, "tags", template.tags != null ? template.tags : new ArrayList<>());
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
            setField(model, "meta", spawn.meta != null ? spawn.meta : new HashMap<>());
            
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
                template.quest_links = convertQuestLinks((List<?>) getField(obj, clazz, "questLinks"));
                template.implementation = (Map<String, Object>) getField(obj, clazz, "implementation");
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
                spawn.quest_links = convertQuestLinks((List<?>) getField(obj, clazz, "questLinks"));
                spawn.meta = (Map<String, Object>) getField(obj, clazz, "meta");

                result.add(spawn);
            }
            Npcshopkeeper.debugLog(LOGGER, "Récupéré {} NpcSpawns depuis l'API.", result.size());
        } catch (Exception e) {
            LOGGER.error("Erreur getNpcSpawns", e);
        }
        return result;
    }

    public static Optional<QuestInfo> getQuest(String questId) {
        if (!isMedievalCoinsLoaded || getQuestDetailsMethod == null) return Optional.empty();
        try {
            Object quest = getQuestDetailsMethod.invoke(null, questId);
            if (quest == null) return Optional.empty();
            Class<?> clazz = quest.getClass();
            return Optional.of(new QuestInfo(
                    getStringField(quest, clazz, "questId"),
                    getStringField(quest, clazz, "name"),
                    localizedText(getField(quest, clazz, "beginText")),
                    localizedText(getField(quest, clazz, "endText"))
            ));
        } catch (Exception e) {
            LOGGER.error("Erreur getQuest({})", questId, e);
            return Optional.empty();
        }
    }

    public static Optional<PlayerQuestState> getActiveQuest(ServerPlayer player, List<String> questIds) {
        if (!isMedievalCoinsLoaded || getActiveQuestsMethod == null || questIds == null) return Optional.empty();
        try {
            List<?> states = (List<?>) getActiveQuestsMethod.invoke(
                    null, player.getGameProfile().getId().toString(), "");
            if (states == null) return Optional.empty();
            for (Object state : states) {
                Class<?> clazz = state.getClass();
                String questId = getStringField(state, clazz, "quest_id");
                if (questIds.contains(questId)) {
                    return Optional.of(new PlayerQuestState(
                            questId,
                            getStringField(state, clazz, "player_id"),
                            getStringField(state, clazz, "status")
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Erreur getActiveQuest pour {}", player.getName().getString(), e);
        }
        return Optional.empty();
    }

    public static boolean startQuest(ServerPlayer player, String questId) {
        return updateQuestStatus(player, questId, "IN_PROGRESS");
    }

    public static boolean completeQuest(ServerPlayer player, PlayerQuestState state) {
        if (!isMedievalCoinsLoaded || completeQuestFromNpcMethod == null) return false;
        try {
            Object result = completeQuestFromNpcMethod.invoke(null, player, state.questId());
            Object status = getField(result, result.getClass(), "status");
            return status != null && "COMPLETED".equals(status.toString());
        } catch (Exception e) {
            LOGGER.error("Erreur completeQuest({})", state.questId(), e);
            return false;
        }
    }

    public static boolean deactivateNpcSpawn(NpcSpawn spawn) {
        if (spawn == null || spawn.spawn_id == null || spawn.spawn_id.isBlank()) return false;
        if (!isMedievalCoinsLoaded) return false;

        try {
            if (deactivateNpcSpawnMethod != null) {
                return (boolean) deactivateNpcSpawnMethod.invoke(null, spawn.spawn_id);
            }
            if (deleteNpcSpawnMethod != null) {
                return (boolean) deleteNpcSpawnMethod.invoke(null, spawn.spawn_id);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur deactivate/deleteNpcSpawn({})", spawn.spawn_id, e);
        }

        NpcSpawn inactive = copySpawn(spawn);
        inactive.active = false;
        return createNpcSpawn(inactive);
    }

    public static boolean showNpcDialogue(ServerPlayer player, String npcName, String text, String texture) {
        if (!isMedievalCoinsLoaded || showNpcDialogueMethod == null) return false;
        try {
            showNpcDialogueMethod.invoke(null, player, npcName, text, texture);
            return true;
        } catch (Exception e) {
            LOGGER.error("Erreur showNpcDialogue({})", npcName, e);
            return false;
        }
    }

    public static boolean openNpcQuestInteractions(ServerPlayer player, String npcId, String npcName, String texture) {
        if (!isMedievalCoinsLoaded || openNpcQuestInteractionsMethod == null) return false;
        try {
            openNpcQuestInteractionsMethod.invoke(null, player, npcId, npcName, texture);
            return true;
        } catch (Exception e) {
            LOGGER.error("Erreur openNpcQuestInteractions({})", npcId, e);
            return false;
        }
    }

    private static boolean updateQuestStatus(ServerPlayer player, String questId, String status) {
        if (!isMedievalCoinsLoaded || sendPostRequestMethod == null || loadConfigMethod == null) return false;
        try {
            Object profile = getPlayerMethod.invoke(null, player.getGameProfile().getId().toString());
            String playerId = getStringField(profile, profile.getClass(), "id");
            Object config = loadConfigMethod.invoke(null);
            String apiUrl = getStringField(config, configClass, "apiUrl");
            String apiKey = getStringField(config, configClass, "apiKey");
            if (apiUrl.endsWith("/")) apiUrl = apiUrl.substring(0, apiUrl.length() - 1);

            JsonObject body = new JsonObject();
            body.addProperty("quest_id", questId);
            body.addProperty("status", status);
            sendPostRequestMethod.invoke(null, apiUrl + "/quests/player/" + playerId + "/update/", apiKey, body);
            return true;
        } catch (Exception e) {
            LOGGER.error("Erreur updateQuestStatus({}, {})", questId, status, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static String localizedText(Object value) {
        if (!(value instanceof Map<?, ?> map)) return "";
        Object text = map.get("fr");
        if (text == null) text = map.get("en");
        return text == null ? "" : text.toString();
    }

    public record QuestInfo(String id, String name, String beginText, String endText) {
    }

    public record PlayerQuestState(String questId, String playerId, String status) {
    }

    public record XpReferenceInfo(String job, float amount) {
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

    public static Optional<NpcSpawn> findNpcSpawn(String spawnId) {
        if (spawnId == null || spawnId.isBlank()) return Optional.empty();
        return getNpcSpawns().stream()
                .filter(spawn -> spawnId.equals(spawn.spawn_id))
                .findFirst();
    }

    private static NpcSpawn copySpawn(NpcSpawn source) {
        NpcSpawn copy = new NpcSpawn();
        copy.spawn_id = source.spawn_id;
        copy.npc_id = source.npc_id;
        copy.world = source.world;
        copy.x = source.x;
        copy.y = source.y;
        copy.z = source.z;
        copy.yaw = source.yaw;
        copy.pitch = source.pitch;
        copy.spawn_rule = source.spawn_rule;
        copy.active = source.active;
        copy.npc_name = source.npc_name;
        copy.npc_type = source.npc_type;
        copy.npc_skin = source.npc_skin;
        copy.dialogue = source.dialogue;
        copy.quest_links = source.quest_links;
        copy.meta = source.meta;
        return copy;
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
            spawn.quest_links = convertQuestLinks((List<?>) getField(obj, clazz, "questLinks"));
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

    private static Object unwrapOptional(Object value) {
        if (value instanceof Optional<?> optional) return optional.orElse(null);
        return value;
    }

    private static Optional<Integer> extractInteger(Object value, String... propertyNames) {
        value = unwrapOptional(value);
        if (value instanceof Number number) return Optional.of(number.intValue());
        if (value == null) return Optional.empty();
        if (value instanceof java.util.OptionalInt optional) return optional.isPresent()
                ? Optional.of(optional.getAsInt())
                : Optional.empty();
        if (value instanceof java.util.OptionalLong optional) return optional.isPresent()
                ? Optional.of((int) optional.getAsLong())
                : Optional.empty();
        if (value instanceof java.util.OptionalDouble optional) return optional.isPresent()
                ? Optional.of((int) optional.getAsDouble())
                : Optional.empty();

        for (String propertyName : propertyNames) {
            Object property = getProperty(value, propertyName);
            if (property instanceof Number number) return Optional.of(number.intValue());
        }
        return Optional.empty();
    }

    private static Optional<XpReferenceInfo> extractXpReference(Object value, String fallbackJob) {
        value = unwrapOptional(value);
        if (value instanceof Number number) {
            return Optional.of(new XpReferenceInfo(fallbackJob == null ? "" : fallbackJob, number.floatValue()));
        }
        if (value == null) return Optional.empty();

        Optional<Integer> xpAmount = extractInteger(value, "xpAmount", "xp", "amount", "value");
        if (xpAmount.isEmpty()) return Optional.empty();

        Object job = getProperty(value, "job");
        String resolvedJob = job != null && !job.toString().isBlank()
                ? job.toString()
                : fallbackJob == null ? "" : fallbackJob;
        return Optional.of(new XpReferenceInfo(resolvedJob, xpAmount.get()));
    }

    private static Object getProperty(Object obj, String name) {
        if (obj == null) return null;
        try {
            Method method = obj.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(obj);
        } catch (Exception ignored) {
            return getField(obj, obj.getClass(), name);
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

    @SuppressWarnings("unchecked")
    private static List<QuestLink> convertQuestLinks(List<?> links) {
        List<QuestLink> result = new ArrayList<>();
        if (links == null) return result;
        for (Object value : links) {
            if (value == null) continue;
            QuestLink link = new QuestLink();
            Class<?> clazz = value.getClass();
            link.quest_id = getStringField(value, clazz, "questId");
            link.name = getStringField(value, clazz, "name");
            link.roles = (List<String>) getField(value, clazz, "roles");
            result.add(link);
        }
        return result;
    }
}
