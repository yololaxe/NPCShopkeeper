package fr.renblood.npcshopkeeper.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
@Mod.EventBusSubscriber
public class WorldEventHandler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            // Chemin vers le dossier de sauvegarde du monde
            File worldSaveFolder = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper").toFile();
            File jsonFile = new File(worldSaveFolder, "trade.json");

            // Vérifier si le fichier existe déjà
            if (!jsonFile.exists()) {
                try {
                    // Créer le dossier npcshopkeeper s'il n'existe pas
                    if (!worldSaveFolder.exists()) {
                        worldSaveFolder.mkdirs();
                    }

                    // Créer le fichier trade.json
                    jsonFile.createNewFile();

                    // Initialiser avec du contenu JSON par défaut (vide par exemple)
                    FileWriter writer = new FileWriter(jsonFile);
                    writer.write("{\"trades\": []}");  // Contenu JSON par défaut
                    writer.close();

                    System.out.println("Le fichier trade.json a été créé dans le dossier du monde.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}