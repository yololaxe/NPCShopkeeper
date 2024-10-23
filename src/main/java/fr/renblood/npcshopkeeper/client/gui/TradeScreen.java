package fr.renblood.npcshopkeeper.client.gui;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.network.TradeButtonMessage;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;


import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {
	private final static HashMap<String, Object> guistate = TradeMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_check;

	public TradeScreen(TradeMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 200;
		this.imageHeight = 166;
	}

	private static final ResourceLocation texture = new ResourceLocation("npcshopkeeper:textures/screens/trade.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
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
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void init() {
		super.init();
		imagebutton_check = new ImageButton(this.leftPos + 209, this.topPos + 65, 51, 34, 0, 0, 34, new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_check.png"), 51, 68, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(new TradeButtonMessage(0, x, y, z));
				TradeButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_check", imagebutton_check);
		this.addRenderableWidget(imagebutton_check);
	}
}

