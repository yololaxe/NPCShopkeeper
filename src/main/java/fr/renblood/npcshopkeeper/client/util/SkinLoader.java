package fr.renblood.npcshopkeeper.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.platform.NativeImage;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinLoader {
    private static final Logger LOGGER = LogManager.getLogger(SkinLoader.class);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Map<String, ResourceLocation> SKIN_CACHE = new HashMap<>();
    private static final Map<String, Boolean> LOADING_STATUS = new HashMap<>();
    private static final String TEXTURE_HASH_PATTERN = "^[0-9a-fA-F]{64}$";
    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";

    public static ResourceLocation getSkin(String skin) {
        if (skin == null || skin.isBlank()) {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        String key = normalizeSkinKey(skin);
        if (SKIN_CACHE.containsKey(key)) {
            return SKIN_CACHE.get(key);
        }

        if (LOADING_STATUS.getOrDefault(key, false)) {
            return DefaultPlayerSkin.getDefaultSkin();
        }

        LOADING_STATUS.put(key, true);
        if (isTextureHash(skin) || isTextureUrl(skin)) {
            loadTextureUrlAsync(key, TEXTURE_URL_PREFIX + extractTextureHash(skin));
        } else if (isImageUrl(skin)) {
            loadTextureUrlAsync(key, skin.trim());
        } else {
            loadPlayerSkinAsync(key, skin.trim());
        }

        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static void loadTextureUrlAsync(String key, String textureUrl) {
        EXECUTOR.submit(() -> {
            if (textureUrl == null || textureUrl.isBlank()) {
                LOADING_STATUS.put(key, false);
                return;
            }

            try {
                URL url = new URL(textureUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "NpcShopkeeper-Mod");

                if (connection.getResponseCode() != 200) {
                    LOGGER.warn("Texture introuvable {} (code {}).", textureUrl, connection.getResponseCode());
                    LOADING_STATUS.put(key, false);
                    return;
                }

                try (InputStream stream = connection.getInputStream()) {
                    NativeImage image = normalizePlayerSkin(NativeImage.read(stream));
                    if (image == null) {
                        LOGGER.warn("Texture ignoree: {} n'est pas un skin joueur 64x64 ou 64x32.", textureUrl);
                        LOADING_STATUS.put(key, false);
                        return;
                    }
                    ResourceLocation location = new ResourceLocation(
                            Npcshopkeeper.MODID,
                            "skins/" + Integer.toHexString(textureUrl.hashCode())
                    );
                    Minecraft.getInstance().execute(() -> {
                        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                        SKIN_CACHE.put(key, location);
                        LOADING_STATUS.put(key, false);
                        LOGGER.info("Texture skin chargee depuis {}.", textureUrl);
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Erreur lors du chargement de la texture {}.", textureUrl, e);
                LOADING_STATUS.put(key, false);
            }
        });
    }

    private static void loadPlayerSkinAsync(String key, String username) {
        EXECUTOR.submit(() -> {
            try {
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

                    Minecraft.getInstance().execute(() -> {
                        SkinManager skinManager = Minecraft.getInstance().getSkinManager();
                        skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                            if (type == MinecraftProfileTexture.Type.SKIN) {
                                SKIN_CACHE.put(key, location);
                                LOADING_STATUS.put(key, false);
                                LOGGER.info("Skin charge pour {} via Ashcon.", username);
                            }
                        }, true);
                    });
                } else {
                    LOGGER.warn("Ashcon API echouee ({}), tentative Mojang API pour {}.", connection.getResponseCode(), username);
                    loadSkinFromMojang(key, username);
                }
            } catch (Exception e) {
                LOGGER.error("Erreur lors du chargement du skin Ashcon pour {}.", username, e);
                loadSkinFromMojang(key, username);
            }
        });
    }

    private static void loadSkinFromMojang(String key, String username) {
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
                    skinManager.registerSkins(profile, (type, location, profileTexture) -> {
                        if (type == MinecraftProfileTexture.Type.SKIN) {
                            SKIN_CACHE.put(key, location);
                            LOADING_STATUS.put(key, false);
                            LOGGER.info("Skin charge pour {} via Mojang.", username);
                        }
                    }, true);
                });
            } else {
                LOGGER.warn("Impossible de trouver le joueur {} (code {}).", username, connection.getResponseCode());
                LOADING_STATUS.put(key, false);
            }
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement du skin Mojang pour {}.", username, e);
            LOADING_STATUS.put(key, false);
        }
    }

    private static boolean isTextureHash(String value) {
        return value != null && value.trim().matches(TEXTURE_HASH_PATTERN);
    }

    private static boolean isTextureUrl(String value) {
        return value != null && value.trim().startsWith(TEXTURE_URL_PREFIX)
                && isTextureHash(extractTextureHash(value));
    }

    private static boolean isImageUrl(String value) {
        if (value == null) return false;
        String lower = value.trim().toLowerCase(Locale.ROOT);
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && (lower.endsWith(".png") || lower.contains(".png?"));
    }

    private static String extractTextureHash(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.startsWith(TEXTURE_URL_PREFIX)) {
            trimmed = trimmed.substring(TEXTURE_URL_PREFIX.length());
            int queryIndex = trimmed.indexOf('?');
            if (queryIndex >= 0) trimmed = trimmed.substring(0, queryIndex);
            int slashIndex = trimmed.indexOf('/');
            if (slashIndex >= 0) trimmed = trimmed.substring(0, slashIndex);
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private static String normalizeSkinKey(String skin) {
        String trimmed = skin.trim();
        if (isTextureHash(trimmed) || isTextureUrl(trimmed)) {
            return "texture:" + extractTextureHash(trimmed);
        }
        if (isImageUrl(trimmed)) {
            return "url:" + trimmed;
        }
        return "player:" + trimmed.toLowerCase(Locale.ROOT);
    }

    private static NativeImage normalizePlayerSkin(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width == 64 && height == 64) {
            setNoAlphaForBaseSkin(image);
            return image;
        }
        if (width != 64 || height != 32) {
            image.close();
            return null;
        }

        NativeImage normalized = new NativeImage(64, 64, true);
        copyRect(image, normalized, 0, 0, 0, 0, 64, 32, false, false);
        image.close();

        copyRect(normalized, normalized, 4, 16, 20, 48, 4, 4, true, false);
        copyRect(normalized, normalized, 8, 16, 24, 48, 4, 4, true, false);
        copyRect(normalized, normalized, 0, 20, 24, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 4, 20, 20, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 8, 20, 16, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 12, 20, 28, 52, 4, 12, true, false);

        copyRect(normalized, normalized, 44, 16, 36, 48, 4, 4, true, false);
        copyRect(normalized, normalized, 48, 16, 40, 48, 4, 4, true, false);
        copyRect(normalized, normalized, 40, 20, 40, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 44, 20, 36, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 48, 20, 32, 52, 4, 12, true, false);
        copyRect(normalized, normalized, 52, 20, 44, 52, 4, 12, true, false);

        setNoAlphaForBaseSkin(normalized);
        return normalized;
    }

    private static void copyRect(NativeImage source, NativeImage target, int sourceX, int sourceY,
                                 int targetX, int targetY, int width, int height,
                                 boolean mirrorX, boolean mirrorY) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int readX = sourceX + (mirrorX ? width - 1 - x : x);
                int readY = sourceY + (mirrorY ? height - 1 - y : y);
                target.setPixelRGBA(targetX + x, targetY + y, source.getPixelRGBA(readX, readY));
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                image.setPixelRGBA(x, y, image.getPixelRGBA(x, y) | 0xFF000000);
            }
        }
    }

    private static void setNoAlphaForBaseSkin(NativeImage image) {
        setNoAlpha(image, 0, 0, 32, 16);      // Head base
        setNoAlpha(image, 16, 16, 40, 32);    // Body, right arm, right leg base
        setNoAlpha(image, 0, 48, 16, 64);     // Left leg base
        setNoAlpha(image, 16, 48, 32, 64);    // Left arm base
    }
}
