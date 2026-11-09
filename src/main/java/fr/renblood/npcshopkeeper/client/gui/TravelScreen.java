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
    private static final int PAGE_SIZE = 5;
    private int currentPage = 0;
    private int pageCount = 1;

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

        List<Port> destinations = PortManager.getAllPorts().stream()
                .filter(port -> !port.getName().equals(currentPortName))
                .toList();
        pageCount = Math.max(1, (destinations.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        currentPage = Math.max(0, Math.min(currentPage, pageCount - 1));

        if (destinations.isEmpty()) {
            Button empty = Button.builder(Component.translatable("gui.npcshopkeeper.travel.no_destinations"), button -> {})
                    .bounds(x + 38, y + 82, 200, 22).build();
            empty.active = false;
            addRenderableWidget(empty);
            return;
        }

        int buttonWidth = 190;
        int buttonX = x + (imageWidth - buttonWidth) / 2;
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(destinations.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            Port port = destinations.get(index);
            int costInIron = calculateCost(port);
            String costText = formatCost(costInIron);
            addRenderableWidget(Button.builder(
                    Component.translatable("gui.npcshopkeeper.travel.port_button", port.getName(), costText),
                    button -> {
                        PacketHandler.sendToServer(new TravelPacket(port.getName(), costInIron));
                        onClose();
                    })
                    .bounds(buttonX, y + 30 + (index - start) * 25, buttonWidth, 20)
                    .build());
        }

        Button previous = Button.builder(Component.translatable("gui.npcshopkeeper.travel.previous"), button -> {
                    currentPage--;
                    rebuildWidgets();
                }).bounds(x + 8, y + 166, 68, 20).build();
        previous.active = currentPage > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.translatable("gui.npcshopkeeper.travel.next"), button -> {
                    currentPage++;
                    rebuildWidgets();
                }).bounds(x + imageWidth - 76, y + 166, 68, 20).build();
        next.active = currentPage < pageCount - 1;
        addRenderableWidget(next);
    }
    private int calculateCost(Port destination) {
        if (Minecraft.getInstance().player == null) return 0;
        
        double distance = Math.sqrt(Minecraft.getInstance().player.blockPosition().distSqr(destination.getPos()));
        
        // Utilisation du prix configuré (par défaut 50)
        int blocksPerIron = Math.max(1, PortManager.getBlocksPerIron());
        
        int cost = (int) Math.ceil(distance / (double)blocksPerIron);
        return Math.max(1, cost);
    }

    private String formatCost(int ironCost) {
        if (ironCost >= 64 * 64 * 64) { // Gold
            int gold = ironCost / (64 * 64 * 64);
            return coinLabel("gold", gold);
        } else if (ironCost >= 64 * 64) { // Silver
            int silver = ironCost / (64 * 64);
            return coinLabel("silver", silver);
        } else if (ironCost >= 64) { // Bronze
            int bronze = ironCost / 64;
            return coinLabel("bronze", bronze);
        } else { // Iron
            return coinLabel("iron", ironCost);
        }
    }

    private String coinLabel(String type, int amount) {
        String quantity = amount == 1 ? "one" : "other";
        return Component.translatable("gui.npcshopkeeper.travel.currency." + type + "." + quantity, amount).getString();
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
        Component page = Component.translatable("gui.npcshopkeeper.travel.page", currentPage + 1, pageCount);
        guiGraphics.drawCenteredString(this.font, page, this.imageWidth / 2, 172, 0xFF5A5147);
    }
}
