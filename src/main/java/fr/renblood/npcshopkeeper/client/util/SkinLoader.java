package fr.renblood.npcshopkeeper.client.util;

import com.mojang.blaze3d.platform.NativeImage;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.skin.SkinLoadGate;
import fr.renblood.npcshopkeeper.skin.SkinRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Rendering stays non-blocking; all HTTP and disk work is bounded/off-thread. */
public final class SkinLoader {
    private static final Logger LOGGER=LogManager.getLogger(SkinLoader.class);
    private static final Map<String,ResourceLocation> CACHE=new ConcurrentHashMap<>();
    private static final SkinLoadGate GATE=new SkinLoadGate();
    private static final ThreadPoolExecutor EXECUTOR=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32),task->{Thread thread=new Thread(task,"Renblood-Skins");thread.setDaemon(true);return thread;});
    private static SkinRepository repository;

    public static ResourceLocation getSkin(String skin) {
        if(skin==null||skin.isBlank())return DefaultPlayerSkin.getDefaultSkin();
        String key=skin.trim();
        ResourceLocation cached=CACHE.get(key);
        if(cached!=null)return cached;
        if(!GATE.begin(key,System.currentTimeMillis()))return DefaultPlayerSkin.getDefaultSkin();
        if(repository==null)repository=new SkinRepository(Minecraft.getInstance().gameDirectory.toPath().resolve("cache/npcshopkeeper/skins-v2"));
        try{EXECUTOR.execute(()->load(key));}
        catch(java.util.concurrent.RejectedExecutionException busy){GATE.failure(key,System.currentTimeMillis(),30_000);}
        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static void load(String key) {
        NativeImage image=null;
        try {
            var cached=repository.cached(key);
            if(cached!=null){
                image=normalizePlayerSkin(NativeImage.read(new ByteArrayInputStream(cached.bytes())));
                if(image!=null){register(key,image,cached.fresh());image=null;if(cached.fresh())return;}
            }
            byte[] bytes=repository.load(key);
            image=normalizePlayerSkin(NativeImage.read(new ByteArrayInputStream(bytes)));
            if(image==null)throw new java.io.IOException("Skin incompatible.");
            register(key,image,true);image=null;

        }catch(Exception error){
            if(image!=null)image.close();
            long delay=error instanceof SkinRepository.RateLimited limited?limited.retryMs:error instanceof SkinRepository.InvalidSource?600_000:30_000;
            GATE.failure(key,System.currentTimeMillis(),delay);
            // One message per attempt, never one request/log line per rendered frame.
            LOGGER.warn("Skin indisponible pour {} : {} Nouvelle tentative différée.",key,error.getMessage());
        }
    }

    private static void register(String key,NativeImage ready,boolean finished){
        Minecraft.getInstance().execute(()->{
            try {
                ResourceLocation location=new ResourceLocation(Npcshopkeeper.MODID,"skins/"+java.util.UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());
                Minecraft.getInstance().getTextureManager().register(location,new DynamicTexture(ready));
                if(CACHE.size()>=512&&!CACHE.containsKey(key)){String oldest=CACHE.keySet().iterator().next();ResourceLocation removed=CACHE.remove(oldest);if(removed!=null)Minecraft.getInstance().getTextureManager().release(removed);}
                CACHE.put(key,location);if(finished)GATE.success(key);
            }catch(Exception registration){ready.close();if(finished)GATE.failure(key,System.currentTimeMillis(),30_000);LOGGER.warn("Enregistrement du skin différé pour {}.",key,registration);}
        });
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
