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

import java.util.ArrayList;
import java.util.List;

public class CreateNpcScreen extends AbstractContainerScreen<CreateNpcMenu> {
    // Nouvelle texture personnalisée
    private static final ResourceLocation TEXTURE = new ResourceLocation("npcshopkeeper", "textures/screens/create_npc.png");

    EditBox nameBox;
    EditBox skinBox;
    Checkbox isShopkeeperCheckbox;
    
    EditBox text1Box;
    EditBox text2Box;
    EditBox text3Box;
    
    Button createButton;

    public CreateNpcScreen(CreateNpcMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222; // Taille standard pour correspondre à la texture
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;

        // Champ Nom
        this.nameBox = new EditBox(this.font, centerX + 10, centerY + 20, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.name_placeholder"));
        this.nameBox.setMaxLength(32);
        this.addRenderableWidget(this.nameBox);

        // Champ Skin (URL ou Pseudo)
        this.skinBox = new EditBox(this.font, centerX + 10, centerY + 50, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.skin_placeholder"));
        this.skinBox.setMaxLength(256);
        this.addRenderableWidget(this.skinBox);

        // Checkbox Shopkeeper
        this.isShopkeeperCheckbox = new Checkbox(centerX + 10, centerY + 80, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.is_shopkeeper"), true);
        this.addRenderableWidget(this.isShopkeeperCheckbox);
        
        // Champs Textes
        this.text1Box = new EditBox(this.font, centerX + 10, centerY + 110, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text1"));
        this.text1Box.setMaxLength(256);
        this.addRenderableWidget(this.text1Box);
        
        this.text2Box = new EditBox(this.font, centerX + 10, centerY + 135, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text2"));
        this.text2Box.setMaxLength(256);
        this.addRenderableWidget(this.text2Box);
        
        this.text3Box = new EditBox(this.font, centerX + 10, centerY + 160, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text3"));
        this.text3Box.setMaxLength(256);
        this.addRenderableWidget(this.text3Box);

        // Bouton Créer
        this.createButton = Button.builder(Component.translatable("gui.npcshopkeeper.create_npc.create_button"), button -> {
            String name = nameBox.getValue();
            String skin = skinBox.getValue();
            boolean isShopkeeper = isShopkeeperCheckbox.selected();
            
            List<String> texts = new ArrayList<>();
            if (!text1Box.getValue().isEmpty()) texts.add(text1Box.getValue());
            if (!text2Box.getValue().isEmpty()) texts.add(text2Box.getValue());
            if (!text3Box.getValue().isEmpty()) texts.add(text3Box.getValue());

            if (!name.isEmpty() && !skin.isEmpty()) {
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new CreateNpcButtonMessage(name, skin, isShopkeeper, texts));
                this.onClose();
            }
        }).bounds(centerX + 10, centerY + 190, 100, 20).build();
        this.addRenderableWidget(this.createButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        int centerX = (this.width - this.imageWidth) / 2;
        int centerY = (this.height - this.imageHeight) / 2;
        
        guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_npc.label_name"), centerX + 10, centerY + 10, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_npc.label_skin"), centerX + 10, centerY + 40, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Utilisation de la surcharge blit qui prend la taille de la texture source (176, 222)
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }
    
    @Override
    public boolean keyPressed(int key, int b, int c) {
        if (key == 256) {
            this.minecraft.player.closeContainer();
            return true;
        }
        if (nameBox.isFocused()) return nameBox.keyPressed(key, b, c);
        if (skinBox.isFocused()) return skinBox.keyPressed(key, b, c);
        if (text1Box.isFocused()) return text1Box.keyPressed(key, b, c);
        if (text2Box.isFocused()) return text2Box.keyPressed(key, b, c);
        if (text3Box.isFocused()) return text3Box.keyPressed(key, b, c);
        return super.keyPressed(key, b, c);
    }
}
