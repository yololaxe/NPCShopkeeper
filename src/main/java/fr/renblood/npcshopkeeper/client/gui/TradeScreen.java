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
import fr.renblood.npcshopkeeper.client.economy.AdaptiveEconomyScreen;
import fr.renblood.npcshopkeeper.client.economy.EconomyUi;
import net.minecraft.client.gui.GuiGraphics;


import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class TradeScreen extends AdaptiveEconomyScreen<TradeMenu> {
	private final static HashMap<String, Object> guistate = TradeMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_check;
	ImageButton imagebutton_red_cross_mc;

	public TradeScreen(TradeMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 276;
		this.imageHeight = 196;
	}

	private static final ResourceLocation texture = new ResourceLocation("npcshopkeeper:textures/screens/trade.png");

	@Override
	public void renderLogical(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.renderLogical(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
        guiGraphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF171713);
        guiGraphics.fill(leftPos+201,topPos+12,leftPos+202,topPos+imageHeight-12,0xFF615638);
        guiGraphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+2,EconomyUi.GOLD);
        for(var slot:menu.slots){guiGraphics.fill(leftPos+slot.x-1,topPos+slot.y-1,leftPos+slot.x+17,topPos+slot.y+17,0xFF4A4435);guiGraphics.fill(leftPos+slot.x,topPos+slot.y,leftPos+slot.x+16,topPos+slot.y+16,0xFF11110F);}
        guiGraphics.drawString(font,"EN LIQUIDE",leftPos+209,topPos+40,EconomyUi.GOLD,false);
        if(imagebutton_check!=null)imagebutton_check.active=!EconomyUi.pending();
        if(imagebutton_red_cross_mc!=null)imagebutton_red_cross_mc.active=!EconomyUi.pending();
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
		// Afficher le titre du trade (récupéré depuis le menu)
		if (this.menu.tradeName != null && !this.menu.tradeName.isEmpty()) {
			guiGraphics.drawString(this.font, Component.literal(font.plainSubstrByWidth(this.menu.tradeName,185)), 8, 6, EconomyUi.TEXT, false);
		}
		guiGraphics.drawWordWrap(font, Component.translatable("gui.npcshopkeeper.trade.close_hint"), 209, 168, 51, EconomyUi.MUTED);
        // Afficher le nom du PNJ si disponible
		if (this.menu.npcName != null && !this.menu.npcName.isEmpty()) {
			guiGraphics.drawString(this.font, Component.literal(font.plainSubstrByWidth(this.menu.npcName,185)), 8, 74, EconomyUi.TEXT, false);
		}
	}

	@Override
	public void init() {
		super.init();
		imagebutton_check = new ImageButton(this.leftPos + 209, this.topPos + 65, 51, 34, 0, 0, 34, new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_check.png"), 51, 68, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(new TradeButtonMessage(0, x, y, z));
			}
		});
        imagebutton_check.setMessage(Component.translatable("gui.npcshopkeeper.trade.confirm"));
        imagebutton_check.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.npcshopkeeper.trade.confirm_help")));
		guistate.put("button:imagebutton_check", imagebutton_check);
		this.addRenderableWidget(imagebutton_check);
		
		imagebutton_red_cross_mc = new ImageButton(this.leftPos + 209, this.topPos + 112, 51, 50, 0, 0, 50, new ResourceLocation("npcshopkeeper:textures/screens/atlas/imagebutton_red_cross_mc.png"), 51, 100, e -> {
			if (true) {
				Npcshopkeeper.PACKET_HANDLER.sendToServer(new TradeButtonMessage(1, x, y, z));
			}
		});
        imagebutton_red_cross_mc.setMessage(Component.translatable("gui.npcshopkeeper.trade.abandon"));
        imagebutton_red_cross_mc.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.npcshopkeeper.trade.abandon_help")));
		guistate.put("button:imagebutton_red_cross_mc", imagebutton_red_cross_mc);
		this.addRenderableWidget(imagebutton_red_cross_mc);
	}
    @Override protected String economyResource(){return "npc-contracts";}
}
