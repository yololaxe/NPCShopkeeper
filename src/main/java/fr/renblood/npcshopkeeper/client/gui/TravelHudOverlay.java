package fr.renblood.npcshopkeeper.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TravelHudOverlay {
    private static long departureTime = -1, secondsAtSync, gameTimeAtSync;

    public static final IGuiOverlay HUD_TRAVEL = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) -> {
        if (departureTime == -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null) return;

        // Simulation time pauses with solo. The server resynchronizes every ten seconds.
        long elapsed = Math.max(0, mc.level.getGameTime() - gameTimeAtSync);
        long seconds = Math.max(0, secondsAtSync - elapsed / 20);
        if (seconds <= 0 || seconds > 60) return;
        String text = String.format("Embarquement · %02d:%02d", seconds / 60, seconds % 60);
        float scale = .8f;
        int x = (int)(screenWidth / scale) - mc.font.width(text) - 12;
        guiGraphics.pose().pushPose(); guiGraphics.pose().scale(scale, scale, 1);
        guiGraphics.fill(x - 5, 9, x + mc.font.width(text) + 5, 24, 0x99101D25);
        guiGraphics.drawString(mc.font, text, x, 12, 0xFFE2BE77, false);
        guiGraphics.pose().popPose();
    };

    public static void setDepartureTime(long time, long seconds) {
        departureTime = time;
        secondsAtSync = seconds;
        var level = Minecraft.getInstance().level;
        gameTimeAtSync = level == null ? 0 : level.getGameTime();
    }
}
