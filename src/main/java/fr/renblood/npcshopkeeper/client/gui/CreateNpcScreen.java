package fr.renblood.npcshopkeeper.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.CreateNpcButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.CreateNpcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CreateNpcScreen extends AbstractContainerScreen<CreateNpcMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");

    EditBox nameBox;
    EditBox skinBox;
    Checkbox isShopkeeperCheckbox;
    Button createButton;

    public CreateNpcScreen(CreateNpcMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 180;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;

        // Champ Nom
        this.nameBox = new EditBox(this.font, centerX + 10, centerY + 20, 150, 20, Component.literal("Nom du PNJ"));
        this.nameBox.setMaxLength(32);
        this.addRenderableWidget(this.nameBox);

        // Champ Skin (URL ou Pseudo)
        this.skinBox = new EditBox(this.font, centerX + 10, centerY + 50, 150, 20, Component.literal("Skin (Pseudo ou URL)"));
        this.skinBox.setMaxLength(256);
        this.addRenderableWidget(this.skinBox);

        // Checkbox Shopkeeper
        this.isShopkeeperCheckbox = new Checkbox(centerX + 10, centerY + 80, 150, 20, Component.literal("Est un Shopkeeper ?"), true);
        this.addRenderableWidget(this.isShopkeeperCheckbox);

        // Bouton Créer
        this.createButton = Button.builder(Component.literal("Créer PNJ"), button -> {
            String name = nameBox.getValue();
            String skin = skinBox.getValue();
            boolean isShopkeeper = isShopkeeperCheckbox.selected();

            if (!name.isEmpty() && !skin.isEmpty()) {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new CreateNpcButtonMessage(name, skin, isShopkeeper));
                this.onClose();
            }
        }).bounds(centerX + 10, centerY + 110, 100, 20).build();
        this.addRenderableWidget(this.createButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        guiGraphics.drawString(this.font, "Nom :", centerX + 10, centerY + 10, 0x404040, false);
        guiGraphics.drawString(this.font, "Skin (Pseudo) :", centerX + 10, centerY + 40, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, 3 * 18 + 17); // Fond générique
        guiGraphics.blit(TEXTURE, i, j + 3 * 18 + 17, 0, 126, this.imageWidth, 96);
    }
    
    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (nameBox.isFocused()) return nameBox.keyPressed(key, b, c);
        if (skinBox.isFocused()) return skinBox.keyPressed(key, b, c);
        return super.keyPressed(key, b, c);
    }
}
