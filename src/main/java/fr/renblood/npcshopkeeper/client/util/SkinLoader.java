package fr.renblood.npcshopkeeper.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinLoader {
    private static final Logger LOGGER = LogManager.getLogger(SkinLoader.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static final Map<String, Boolean> LOADING_STATUS = new HashMap<>(); // true = en cours

    public static ResourceLocation getSkin(String username) {
        if (SKIN_CACHE.containsKey(username)) {
            return SKIN_CACHE.get(username);
        }

        if (LOADING_STATUS.getOrDefault(username, false)) {
            return DefaultPlayerSkin.getDefaultSkin(); // En cours de chargement, on retourne Steve
        }

        // Lancer le chargement
        LOADING_STATUS.put(username, true);
        loadSkinAsync(username);

        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static void loadSkinAsync(String username) {
        EXECUTOR.submit(() -> {
            try {
                // 1. Essayer l'API Ashcon (plus robuste, cache intégré, évite le 429 de Mojang)
                URL url = new URL("https://api.ashcon.app/mojang/v2/user/" + username);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "NpcShopkeeper-Mod");
                
                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    String uuidStr = json.get("uuid").getAsString();
                    String name = json.get("username").getAsString();
                    
                    UUID uuid = UUID.fromString(uuidStr);
                    GameProfile profile = new GameProfile(uuid, name);

                    // 2. Charger le skin via le SkinManager de Minecraft avec registerSkins (force le chargement)
                    Minecraft.getInstance().execute(() -> {
                        SkinManager skinManager = Minecraft.getInstance().getSkinManager();
                        
                        // registerSkins lance le chargement asynchrone et appelle le callback quand c'est fini
                        skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                            if (type == MinecraftProfileTexture.Type.SKIN) {
                                SKIN_CACHE.put(username, location);
                                LOADING_STATUS.put(username, false);
                                LOGGER.info("Skin chargé pour : " + username + " (via Ashcon API + registerSkins)");
                            }
                        }, true); // true = requireSecure (généralement true pour les profils officiels)
                    });
                } else {
                    LOGGER.warn("Ashcon API échouée (" + connection.getResponseCode() + "), tentative Mojang API...");
                    loadSkinFromMojang(username);
                }
            } catch (Exception e) {
                LOGGER.error("Erreur lors du chargement du skin (Ashcon) pour " + username, e);
                loadSkinFromMojang(username);
            }
        });
    }

    private static void loadSkinFromMojang(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                String uuidStr = json.get("id").getAsString();
                String name = json.get("name").getAsString();

                UUID uuid = UUID.fromString(uuidStr.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));

                GameProfile profile = new GameProfile(uuid, name);

                Minecraft.getInstance().execute(() -> {
                    SkinManager skinManager = Minecraft.getInstance().getSkinManager();
                    
                    // Utilisation de registerSkins ici aussi
                    skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            SKIN_CACHE.put(username, location);
                            LOADING_STATUS.put(username, false);
                            LOGGER.info("Skin chargé pour : " + username + " (via Mojang API + registerSkins)");
                        }
                    }, true);
                });
            } else {
                LOGGER.warn("Impossible de trouver le joueur : " + username + " (Code: " + connection.getResponseCode() + ")");
                LOADING_STATUS.put(username, false);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement du skin (Mojang) pour " + username, e);
            LOADING_STATUS.put(username, false);
        }
    }
}
