package fr.renblood.npcshopkeeper.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.TravelPacket;
import fr.renblood.npcshopkeeper.world.inventory.TravelMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class TravelScreen extends AbstractContainerScreen<TravelMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("npcshopkeeper", "textures/screens/travel_gui.png");
    private final String currentPortName;

    public TravelScreen(TravelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 206;
        this.inventoryLabelY = this.imageHeight - 94;
        this.currentPortName = menu.getCurrentPortName();
    }

    @Override
    protected void init() {
        super.init();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        List<Port> ports = PortManager.getAllPorts(); 
        
        if (ports.isEmpty()) {
             this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.npcshopkeeper.travel.no_ports"),
                    button -> this.onClose())
                    .bounds(x + (this.imageWidth - 150) / 2, y + 20, 150, 20)
                    .build());
             return;
        }

        int i = 0;
        int buttonWidth = 180;
        int buttonX = x + (this.imageWidth - buttonWidth) / 2; // Centré horizontalement
        int startY = y + 30; // Marge du haut

        for (Port port : ports) {
            if (port.getName().equals(currentPortName)) continue;

            int costInIron = calculateCost(port);
            String costText = formatCost(costInIron);
            
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.npcshopkeeper.travel.port_button", port.getName(), costText),
                    button -> {
                        PacketHandler.sendToServer(new TravelPacket(port.getName(), costInIron));
                        this.onClose();
                    })
                    .bounds(buttonX, startY + (i * 25), buttonWidth, 20)
                    .build());
            i++;
            if (i >= 6) break;
        }
    }

    private int calculateCost(Port destination) {
        if (Minecraft.getInstance().player == null) return 0;
        
        double distance = Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(destination.getPos()));
        
        // Utilisation du prix configuré (par défaut 50)
        int blocksPerIron = 50; // PortManager.getBlocksPerIron(); // Risque de désynchro
        
        int cost = (int) Math.ceil(distance / (double)blocksPerIron);
        return Math.max(1, cost);
    }

    private String formatCost(int ironCost) {
        if (ironCost >= 64 * 64 * 64) { // Gold
            int gold = ironCost / (64 * 64 * 64);
            return gold + " Gold" + (gold > 1 ? "s" : "");
        } else if (ironCost >= 64 * 64) { // Silver
            int silver = ironCost / (64 * 64);
            return silver + " Silver" + (silver > 1 ? "s" : "");
        } else if (ironCost >= 64) { // Bronze
            int bronze = ironCost / 64;
            return bronze + " Bronze" + (bronze > 1 ? "s" : "");
        } else { // Iron
            return ironCost + " Iron" + (ironCost > 1 ? "s" : "");
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // blit(texture, x, y, u, v, width, height, textureWidth, textureHeight)
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Centrer le titre
        Component title = Component.translatable("gui.npcshopkeeper.travel.current_port", currentPortName);
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.imageWidth - titleWidth) / 2, 10, 4210752, false);
    }
}
