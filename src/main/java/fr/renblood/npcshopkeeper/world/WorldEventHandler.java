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

            // Initialisation des fichiers nécessaires
            initializeFile(worldSaveFolder, "trade.json", "{\"trades\": []}");
            initializeFile(worldSaveFolder, "trade_history.json", "{\"history\": []}");
            initializeFile(worldSaveFolder, "price_references.json", "{\"references\": []}");
            initializeFile(worldSaveFolder, "commercial_road.json", "{\"roads\": []}");
            initializeFile(worldSaveFolder, "constant.json", getDefaultConstantJson());
        }
    }

    private static void initializeFile(File folder, String fileName, String defaultContent) {
        File file = new File(folder, fileName);
        if (!file.exists()) {
            try {
                // Créer le dossier npcshopkeeper s'il n'existe pas
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                // Créer le fichier et écrire le contenu par défaut
                file.createNewFile();
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(defaultContent);
                }

                System.out.println("Le fichier " + fileName + " a été créé dans le dossier du monde.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getDefaultConstantJson() {
        // Contenu JSON par defaut pour constant.json
        return """
        {
          "pnjs": {
            "Bingo": {
              "Texture": "textures/entity/bingo.png",
              "Texts": [
                "Bienvenue dans le jardin ! Pret a commencer ?",
                "Tu devrais planter des melons, ils poussent vite !",
                "Cet endroit est parfait pour se detendre."
              ]
            },
            "George": {
              "Texture": "textures/entity/george.png",
              "Texts": [
                "J'aime ce que tu as fait ici.",
                "Les animaux adorent cet endroit.",
                "Tu pourrais ajouter une serre ?"
              ]
            },
            "Diana": {
              "Texture": "textures/entity/diana.png",
              "Texts": [
                "Tes recoltes sont impressionnantes.",
                "Les fruits de ton travail sont magnifiques.",
                "Tu prends vraiment soin de ton jardin."
              ]
            },
            "Bazaar": {
              "Texture": "textures/entity/bazaar.png",
              "Texts": [
                "Besoin d'aide pour le jardin ?",
                "Je peux te fournir ce qu'il te faut.",
                "Pense a ajouter des fleurs rares ici."
              ]
            },
            "Jerry": {
              "Texture": "textures/entity/jerry.png",
              "Texts": [
                "C'est agreable de voir le jardin grandir.",
                "Ton jardin attire beaucoup d'attention.",
                "N'oublie pas de nourrir tes animaux."
              ]
            },
            "Derpy": {
              "Texture": "textures/entity/derpy.png",
              "Texts": [
                "Pourquoi pas planter du ble ici ?",
                "Les betteraves poussent bien ici.",
                "Et si tu essayais de cultiver des champignons ?"
              ]
            },
            "Paul": {
              "Texture": "textures/entity/paul.png",
              "Texts": [
                "Tes recoltes de carottes sont incroyables !",
                "Tu devrais vendre tes surplus.",
                "Un bon fermier connait ses sols."
              ]
            },
            "Sam": {
              "Texture": "textures/entity/sam.png",
              "Texts": [
                "Ce jardin est parfait pour se relaxer.",
                "J'aime ton organisation.",
                "Tu devrais peut-etre installer un bassin."
              ]
            },
            "Tia": {
              "Texture": "textures/entity/tia.png",
              "Texts": [
                "As-tu besoin d'engrais ?",
                "Ce sol est parfait pour planter.",
                "Je peux t'aider avec tes plantes."
              ]
            },
            "Adventurer": {
              "Texture": "textures/entity/adventurer.png",
              "Texts": [
                "Ce jardin est un endroit calme.",
                "Il offre une pause loin de l'agitation.",
                "Ces fruits sont parfaits pour mes voyages."
              ]
            }
          }
        }
        """;
    }

}