package fr.renblood.npcshopkeeper.client.renderer;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class TradeNpcRenderer extends HumanoidMobRenderer<TradeNpcEntity, PlayerModel<TradeNpcEntity>> {

    private static final Logger LOGGER = LogManager.getLogger(TradeNpcRenderer.class);

    public TradeNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 1F);
//        LOGGER.info("✅ TradeNpcRenderer initialisé.");
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(TradeNpcEntity entity) {
        String texturePath = entity.getTexture(); // ex: "textures/entity/paul.png"
//        LOGGER.info("🔍 Chargement de la texture pour l'entité : " + entity.getName().getString());
//        LOGGER.info("🧵 UUID de l'entité : " + entity.getUUID());
//        LOGGER.info("📦 Texture spécifiée : " + texturePath);

        if (texturePath == null || texturePath.isEmpty()) {
//            LOGGER.warn("⚠️ Texture vide ou nulle, texture par défaut utilisée.");
            return new ResourceLocation(Npcshopkeeper.MODID, "textures/entity/banker.png");
        }

//        if (texturePath.startsWith("textures/")) {
//            texturePath = texturePath.substring("textures/".length());
//        }

        ResourceLocation finalTexture = new ResourceLocation(Npcshopkeeper.MODID, texturePath);
//        LOGGER.info("✅ Texture utilisée : " + finalTexture);
        return finalTexture;
    }

}
