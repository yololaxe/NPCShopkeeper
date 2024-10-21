package fr.renblood.npcshopkeeper.client.gui;

import fr.renblood.npcshopkeeper.Npcshopkeeper;

import fr.renblood.npcshopkeeper.network.CreateTradeButtonMessage;
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

import net.mcreator.npcshopkeeper.world.inventory.CreateTradeMenu;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class CreateTradeScreen extends AbstractContainerScreen<CreateTradeMenu> {
	private final static HashMap<String, Object> guistate = CreateTradeMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	EditBox trade_name;
	ImageButton imagebutton_check;

	public CreateTradeScreen(CreateTradeMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 276;
		this.imageHeight = 206;
	}

	private static final ResourceLocation texture = new ResourceLocation("npcshopkeeper:textures/screens/create_trade.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		trade_name.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("npcshopkeeper:textures/screens/transform.png"), this.leftPos + 164, this.topPos + 12, 0, 0, 32, 64, 32, 64);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		if (trade_name.isFocused())
			return trade_name.keyPressed(key, b, c);
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		trade_name.tick();
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		String trade_nameValue = trade_name.getValue();
		super.resize(minecraft, width, height);
		trade_name.setValue(trade_nameValue);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_empty"), 41, 24, -16777216, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_empty1"), 41, 53, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_empty2"), 119, 24, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_empty3"), 119, 51, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_category"), 11, 81, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_create_trade"), 11, 5, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.npcshopkeeper.create_trade.label_item"), 235, 39, -12829636, false);
	}

	@Override
	public void init() {
		super.init();
		trade_name = new EditBox(this.font, this.leftPos + 122, this.topPos + 77, 118, 18, Component.translatable("gui.npcshopkeeper.create_trade.trade_name")) {
			@Override
			public void insertText(String text) {
				super.insertText(text);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.create_trade.trade_name").getString());
				else
					setSuggestion(null);
			}

			@Override
			public void moveCursorTo(int pos) {
				super.moveCursorTo(pos);
				if (getValue().isEmpty())
					setSuggestion(Component.translatable("gui.npcshopkeeper.create_trade.trade_name").getString());
				else
					setSuggestion(null);
			}
		};
		trade_name.setSuggestion(Component.translatable("gui.npcshopkeeper.create_trade.trade_name").getString());
		trade_name.setMaxLength(32767);
		guistate.put("text:trade_name", trade_name);
		this.addWidget(this.trade_name);
		imagebutton_check = new ImageButton(this.leftPos + 221, this.topPos + 115, 51, 34, 0, 0, 34, new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_check.png"), 51, 68, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(new CreateTradeButtonMessage(0, x, y, z));
				CreateTradeButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_check", imagebutton_check);
		this.addRenderableWidget(imagebutton_check);
	}
}
