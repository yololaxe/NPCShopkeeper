package fr.renblood.npcshopkeeper.client.gui;

import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class TravelHudOverlay {
    private static long departureTime = -1;

    public static final IGuiOverlay HUD_TRAVEL = (ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) -> {
        if (departureTime == -1) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long currentTime = mc.level.getDayTime() % 24000;
        long timeRemainingTicks = departureTime - currentTime;

        if (timeRemainingTicks <= 0) {
            departureTime = -1; // Reset si le temps est écoulé
            return;
        }

        // Calcul du temps réel restant
        // Formule : SecondesRéelles = TicksJeuRestants * (dayLengthInMinutes / 400.0)
        // Ex: Si jour = 20 min -> ratio = 20/400 = 0.05 (1 tick = 0.05s = 1/20s)
        // Ex: Si jour = 30 min -> ratio = 30/400 = 0.075
        
        double dayLength = PortManager.getDayLengthInMinutes();
        double secondsRemaining = timeRemainingTicks * (dayLength / 400.0);
        
        // Formatage en minutes:secondes
        int minutes = (int) (secondsRemaining / 60);
        int seconds = (int) (secondsRemaining % 60);
        
        String text = String.format("Départ dans : %02d:%02d", minutes, seconds);
        int x = 10;
        int y = screenHeight / 2;

        guiGraphics.drawString(mc.font, Component.literal(text), x, y, 0xFFFFFF);
    };

    public static void setDepartureTime(long time) {
        departureTime = time;
    }
}
