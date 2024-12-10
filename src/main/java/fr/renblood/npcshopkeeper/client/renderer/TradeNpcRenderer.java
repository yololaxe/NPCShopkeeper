package fr.renblood.npcshopkeeper.client.renderer;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.procedures.TradeCommandProcedure;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Npc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TradeNpcRenderer extends HumanoidMobRenderer<TradeNpcEntity, PlayerModel<TradeNpcEntity>> {

    private static final Logger LOGGER = LogManager.getLogger(TradeNpcRenderer.class);
    public TradeNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 1.0F);
    }
    @Override
    public ResourceLocation getTextureLocation(TradeNpcEntity entity) {
        if (entity.getTexture() == null || entity.getTexture().isEmpty()) {
            return new ResourceLocation("minecraft", "textures/entity/zombie.png"); // Texture par défaut en cas de problème
        }
        return new ResourceLocation(Npcshopkeeper.MODID, entity.getTexture());
    }

}