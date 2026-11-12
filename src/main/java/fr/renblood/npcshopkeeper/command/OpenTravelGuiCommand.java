package fr.renblood.npcshopkeeper.command;
import net.minecraft.server.level.ServerPlayer;
import fr.renblood.npcshopkeeper.services.VoyageService;
/** Retained entry point for integrations; every path opens the same voyage map. */
public final class OpenTravelGuiCommand {
    public static void openGui(ServerPlayer player,String currentPortName){VoyageService.open(player,currentPortName,false);}
}
