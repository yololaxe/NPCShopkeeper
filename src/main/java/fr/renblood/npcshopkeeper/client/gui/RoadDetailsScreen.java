package fr.renblood.npcshopkeeper.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.RoadDetailsButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.RoadDetailsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class RoadDetailsScreen extends AbstractContainerScreen<RoadDetailsMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    public RoadDetailsScreen(RoadDetailsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + 3 * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        // Bouton Supprimer la route
        this.addRenderableWidget(Button.builder(Component.translatable("gui.npcshopkeeper.road_details.delete_road"), button -> {
            if (menu.road != null) {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new RoadDetailsButtonMessage(0, menu.road.getId()));
                this.onClose();
            }
        }).bounds(this.leftPos + 7, this.topPos - 25, 100, 20).build());

        // Bouton Téléporter (au premier PNJ ou au premier point)
        this.addRenderableWidget(Button.builder(Component.translatable("gui.npcshopkeeper.road_details.teleport"), button -> {
            if (menu.road != null) {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new RoadDetailsButtonMessage(1, menu.road.getId()));
                this.onClose();
            }
        }).bounds(this.leftPos + 110, this.topPos - 25, 60, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, 3 * 18 + 17);
        guiGraphics.blit(TEXTURE, i, j + 3 * 18 + 17, 0, 126, this.imageWidth, 96);
    }
}
