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
	ImageButton imagebutton_check;

	public NpcShopkeeperWandGuiScreen(NpcShopkeeperWandGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 200;
		this.imageHeight = 166;
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
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
		RenderSystem.disableBlend();
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
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		name.tick();
		points.tick();
		minTimer.tick();
		maxTimer.tick();
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String nameValue = name.getValue();
		String pointsValue = points.getValue();
		String minTimerValue = minTimer.getValue();
		String maxTimerValue = maxTimer.getValue();
		super.resize(minecraft, width, height);
		name.setValue(nameValue);
		points.setValue(pointsValue);
		minTimer.setValue(minTimerValue);
		maxTimer.setValue(maxTimerValue);
	}
	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.label_time_in_minute"), 10, 88, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.label_max_points", getDataSize()), 136, 64, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.npc_number"), 10, 46, -12829636, false);
	}



	@Override
	public void init() {
		super.init();
		name = new EditBox(this.font, this.leftPos + 43, this.topPos + 17, 118, 18, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.name")) {
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
		points = new EditBox(this.font, this.leftPos + 11, this.topPos + 61, 118, 18, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.points")) {
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
		minTimer = new EditBox(this.font, this.leftPos + 11, this.topPos + 104, 118, 18, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.minTimer")) {
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
		maxTimer = new EditBox(this.font, this.leftPos + 11, this.topPos + 131, 118, 18, Component.translatable("gui.npcshopkeeper.npc_shopkeeper_wand_gui.maxTimer")) {
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
		imagebutton_check = new ImageButton(this.leftPos + 208, this.topPos + 59, 51, 34, 0, 0, 34,
				new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_check.png"),
				51, 68, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(
						new NpcShopkeeperWandGuiButtonMessage(0, x, y, z, menu.getCategory()) // Inclure la catégorie
				);
			}
		}
		);

		guistate.put("button:imagebutton_check", imagebutton_check);
		this.addRenderableWidget(imagebutton_check);
	}
}
