package fr.renblood.npcshopkeeper.client.gui;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.NpcShopkeeperWandGuiButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.NpcShopkeeperWandGuiMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;



import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

import static fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getDataSize;

public class NpcShopkeeperWandGuiScreen extends AbstractContainerScreen<NpcShopkeeperWandGuiMenu> {
	private final static HashMap<String, Object> guistate = NpcShopkeeperWandGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	EditBox name;
	EditBox points;
	EditBox minTimer;
	EditBox maxTimer;
	EditBox category;
	private String roadType = "TRADE";
	private String tradeCategory = "";
	private Button roadTypeButton;
	ImageButton imagebutton_check;

	public NpcShopkeeperWandGuiScreen(NpcShopkeeperWandGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 280;
		this.imageHeight = 210;
	}

	private static final ResourceLocation texture = new ResourceLocation("npcshopkeeper:textures/screens/npc_shopkeeper_wand_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		name.render(guiGraphics, mouseX, mouseY, partialTicks);
		points.render(guiGraphics, mouseX, mouseY, partialTicks);
		minTimer.render(guiGraphics, mouseX, mouseY, partialTicks);
		maxTimer.render(guiGraphics, mouseX, mouseY, partialTicks);
		category.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + imageWidth, this.topPos + imageHeight, 0xF0181D23);
		guiGraphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + imageWidth - 4, this.topPos + imageHeight - 4, 0xFF293640);
		guiGraphics.fill(this.leftPos + 10, this.topPos + 38, this.leftPos + imageWidth - 10, this.topPos + imageHeight - 10, 0xFF1E282F);
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (name.isFocused())
			return name.keyPressed(key, b, c);
		if (points.isFocused())
			return points.keyPressed(key, b, c);
		if (minTimer.isFocused())
			return minTimer.keyPressed(key, b, c);
		if (maxTimer.isFocused())
			return maxTimer.keyPressed(key, b, c);
		if (category.isFocused())
			return category.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		name.tick();
		points.tick();
		minTimer.tick();
		maxTimer.tick();
		category.tick();
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String nameValue = name.getValue();
		String pointsValue = points.getValue();
		String minTimerValue = minTimer.getValue();
		String maxTimerValue = maxTimer.getValue();
		String categoryValue = category.getValue();
		String savedRoadType = roadType;
		super.resize(minecraft, width, height);
		name.setValue(nameValue);
		points.setValue(pointsValue);
		minTimer.setValue(minTimerValue);
		maxTimer.setValue(maxTimerValue);
		category.setValue(categoryValue);
		roadType = savedRoadType;
		updateRouteTypeControls();
	}
	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawCenteredString(this.font, "Creation d'une route PNJ", imageWidth / 2, 14, 0xFFF2D49B);
		guiGraphics.drawString(this.font, "Nom", 16, 47, 0xFFE8E0CC, false);
		guiGraphics.drawString(this.font, "Nombre de points (1-32)", 16, 79, 0xFFE8E0CC, false);
		guiGraphics.drawString(this.font, "Delai minimum (secondes)", 16, 111, 0xFFE8E0CC, false);
		guiGraphics.drawString(this.font, "Delai maximum (secondes)", 16, 143, 0xFFE8E0CC, false);
		guiGraphics.drawString(this.font, "Type de route", 150, 79, 0xFFE8E0CC, false);
		guiGraphics.drawString(this.font, roadType.equals("TRADE") ? "Categorie de trade" : "Categorie automatique", 150, 111, 0xFFE8E0CC, false);
	}



	@Override
	public void init() {
		super.init();
		name = new EditBox(this.font, this.leftPos + 16, this.topPos + 56, 118, 18, Component.literal("Nom")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.name").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.name").getString());
				else
					setSuggestion(null);
			}
		};
		name.setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.name").getString());
		name.setMaxLength(32767);
		guistate.put("text:name", name);
		this.addWidget(this.name);
		points = new EditBox(this.font, this.leftPos + 16, this.topPos + 88, 118, 18, Component.literal("Points")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.points").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.points").getString());
				else
					setSuggestion(null);
			}
		};
		points.setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.points").getString());
		points.setMaxLength(32767);
		guistate.put("text:points", points);
		this.addWidget(this.points);
		minTimer = new EditBox(this.font, this.leftPos + 16, this.topPos + 120, 118, 18, Component.literal("Minimum")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.minTimer").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.minTimer").getString());
				else
					setSuggestion(null);
			}
		};
		minTimer.setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.minTimer").getString());
		minTimer.setMaxLength(32767);
		guistate.put("text:minTimer", minTimer);
		this.addWidget(this.minTimer);
		maxTimer = new EditBox(this.font, this.leftPos + 16, this.topPos + 152, 118, 18, Component.literal("Maximum")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.maxTimer").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.maxTimer").getString());
				else
					setSuggestion(null);
			}
		};
		maxTimer.setSuggestion(Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.maxTimer").getString());
		maxTimer.setMaxLength(32767);
		guistate.put("text:maxTimer", maxTimer);
		this.addWidget(this.maxTimer);
		category = new EditBox(this.font, this.leftPos + 150, this.topPos + 120, 114, 18, Component.literal("Categorie"));
		category.setValue(menu.getCategory());
		tradeCategory = menu.getCategory();
		category.setMaxLength(128);
		this.addWidget(category);
		roadTypeButton = Button.builder(Component.empty(), button -> {
			if (roadType.equals("TRADE")) {
				tradeCategory = category.getValue();
				roadType = "INN_MENU";
			} else {
				roadType = "TRADE";
				category.setValue(tradeCategory);
			}
			updateRouteTypeControls();
		}).bounds(this.leftPos + 150, this.topPos + 88, 114, 20).build();
		this.addRenderableWidget(roadTypeButton);
		updateRouteTypeControls();
		imagebutton_check = new ImageButton(this.leftPos + 213, this.topPos + 158, 51, 34, 0, 0, 34,
				new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_check.png"),
				51, 68, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(
						new NpcShopkeeperWandGuiButtonMessage(0, x, y, z, roadType, category.getValue(), name.getValue(), points.getValue(), minTimer.getValue(), maxTimer.getValue())
				);
			}
		}
		);

		guistate.put("button:imagebutton_check", imagebutton_check);
		this.addRenderableWidget(imagebutton_check);
	}

	private void updateRouteTypeControls() {
		if (roadTypeButton != null) {
			roadTypeButton.setMessage(Component.literal(roadType.equals("TRADE") ? "Commerce" : "Menu auberge"));
		}
		if (category != null) {
			category.setEditable(roadType.equals("TRADE"));
			if (roadType.equals("INN_MENU")) category.setValue("inn_menu");
		}
	}
}
