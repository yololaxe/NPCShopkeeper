package fr.renblood.npcshopkeeper.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.CreateNpcButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.CreateNpcMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class CreateNpcScreen extends AbstractContainerScreen<CreateNpcMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("npcshopkeeper", "textures/screens/create_npc.png");

    EditBox nameBox;
    EditBox skinBox;
    
    // Remplacement de la Checkbox par un bouton cyclique pour le Type
    Button typeButton;
    private String currentType = "DECO"; // DECO, SHOPKEEPER, QUEST
    
    // Champs dynamiques
    EditBox text1Box;
    EditBox text2Box;
    EditBox text3Box;
    
    // Champs spécifiques Shopkeeper
    EditBox tradeCategoryBox;
    
    Button createButton;

    public CreateNpcScreen(CreateNpcMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
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

        // Champ Skin
        this.skinBox = new EditBox(this.font, centerX + 10, centerY + 50, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.skin_placeholder"));
        this.skinBox.setMaxLength(256);
        this.addRenderableWidget(this.skinBox);

        // Bouton Type (Cycle)
        this.typeButton = Button.builder(Component.literal("Type: " + currentType), button -> {
            switch (currentType) {
                case "DECO" -> currentType = "SHOPKEEPER";
                case "SHOPKEEPER" -> currentType = "QUEST";
                case "QUEST" -> currentType = "DECO";
            }
            button.setMessage(Component.literal("Type: " + currentType));
            updateVisibility();
        }).bounds(centerX + 10, centerY + 80, 150, 20).build();
        this.addRenderableWidget(this.typeButton);
        
        // Champs Textes (Communs)
        this.text1Box = new EditBox(this.font, centerX + 10, centerY + 110, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text1"));
        this.text1Box.setMaxLength(256);
        this.addRenderableWidget(this.text1Box);
        
        this.text2Box = new EditBox(this.font, centerX + 10, centerY + 135, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text2"));
        this.text2Box.setMaxLength(256);
        this.addRenderableWidget(this.text2Box);
        
        this.text3Box = new EditBox(this.font, centerX + 10, centerY + 160, 150, 20, Component.translatable("gui.npcshopkeeper.create_npc.text3"));
        this.text3Box.setMaxLength(256);
        this.addRenderableWidget(this.text3Box);
        
        // Champ Spécifique Shopkeeper
        this.tradeCategoryBox = new EditBox(this.font, centerX + 10, centerY + 135, 150, 20, Component.literal("Catégorie de Trade"));
        this.tradeCategoryBox.setMaxLength(256);
        this.tradeCategoryBox.setVisible(false); // Caché par défaut
        this.addRenderableWidget(this.tradeCategoryBox);

        // Bouton Créer
        this.createButton = Button.builder(Component.translatable("gui.npcshopkeeper.create_npc.create_button"), button -> {
            String name = nameBox.getValue();
            String skin = skinBox.getValue();
            
            List<String> texts = new ArrayList<>();
            if (!text1Box.getValue().isEmpty()) texts.add(text1Box.getValue());
            if (!text2Box.getValue().isEmpty()) texts.add(text2Box.getValue());
            if (!text3Box.getValue().isEmpty()) texts.add(text3Box.getValue());
            
            // Récupération des données spécifiques
            String category = currentType.equals("SHOPKEEPER") ? tradeCategoryBox.getValue() : "";

            if (!name.isEmpty() && !skin.isEmpty()) {
                // Envoi du packet avec le nouveau format (Type + Category)
                // Note: Il faudra mettre à jour CreateNpcButtonMessage pour supporter ces champs
                Npcshopkeeper.PACKET_HANDLER.sendToServer(new CreateNpcButtonMessage(name, skin, currentType, texts, category));
                this.onClose();
            }
        }).bounds(centerX + 10, centerY + 190, 100, 20).build();
        this.addRenderableWidget(this.createButton);
        
        updateVisibility();
    }
    
    private void updateVisibility() {
        boolean isShop = currentType.equals("SHOPKEEPER");
        boolean isQuest = currentType.equals("QUEST");
        
        // Gestion de l'affichage dynamique
        // Si Shopkeeper, on remplace text2 par tradeCategory pour l'exemple (ou on ajoute un champ)
        // Ici, pour faire simple dans l'espace restreint :
        
        if (isShop) {
            text2Box.setVisible(false);
            tradeCategoryBox.setVisible(true);
        } else {
            text2Box.setVisible(true);
            tradeCategoryBox.setVisible(false);
        }
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
        if (tradeCategoryBox.isFocused()) return tradeCategoryBox.keyPressed(key, b, c);
        return super.keyPressed(key, b, c);
    }
}
