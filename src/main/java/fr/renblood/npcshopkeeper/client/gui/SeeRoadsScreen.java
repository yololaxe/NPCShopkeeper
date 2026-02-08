package fr.renblood.npcshopkeeper.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.network.SeeRoadsButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.SeeRoadsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public class SeeRoadsScreen extends AbstractContainerScreen<SeeRoadsMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    public SeeRoadsScreen(SeeRoadsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 114 + 3 * 18; // Ajuster selon la texture
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int page = this.menu.getPage();
        int totalRoads = Npcshopkeeper.COMMERCIAL_ROADS.size();
        boolean hasNext = (page + 1) * 27 < totalRoads;
        boolean hasPrev = page > 0;

        if (hasPrev) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new SeeRoadsButtonMessage(page - 1));
            }).bounds(this.leftPos, this.topPos - 20, 20, 20).build());
        }

        if (hasNext) {
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new SeeRoadsButtonMessage(page + 1));
            }).bounds(this.leftPos + this.imageWidth - 20, this.topPos - 20, 20, 20).build());
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // Intercepter le clic sur les slots de routes (0 à 26)
        if (slot != null && slot.index >= 0 && slot.index < 27) {
            // Récupérer la route correspondante via le menu (qui a la liste displayedRoads mais elle est privée/locale)
            // On peut tricher en récupérant l'item du slot et en cherchant la route par nom, ou mieux :
            // On envoie l'index du slot au serveur, et le serveur (qui a aussi le menu ouvert) saura quelle route c'est.
            // Mais SeeRoadsButtonMessage attend un ID de route (String) ou une page (int).
            
            // Le plus simple est de récupérer l'item dans le slot, qui est nommé avec le nom de la route.
            if (slot.hasItem()) {
                String roadName = slot.getItem().getHoverName().getString();
                // Trouver la route par nom dans la liste globale (côté client, Npcshopkeeper.COMMERCIAL_ROADS est sync ?)
                // Attention : Npcshopkeeper.COMMERCIAL_ROADS peut être vide sur le client si pas sync.
                // Mais le menu a rempli les slots, donc il a les infos.
                
                // On va chercher dans la liste globale statique (en espérant qu'elle soit sync, sinon le menu n'aurait rien affiché)
                CommercialRoad road = Npcshopkeeper.COMMERCIAL_ROADS.stream()
                        .filter(r -> r.getName().equals(roadName))
                        .findFirst()
                        .orElse(null);
                
                if (road != null) {
                    Npcshopkeeper.PACKET_HANDLER.sendToServer(new SeeRoadsButtonMessage(road.getId()));
                    return; // Ne pas appeler super.slotClicked pour éviter de prendre l'item
                }
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
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
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, 3 * 18 + 17); // Top part
        guiGraphics.blit(TEXTURE, i, j + 3 * 18 + 17, 0, 126, this.imageWidth, 96); // Bottom part
    }
}
