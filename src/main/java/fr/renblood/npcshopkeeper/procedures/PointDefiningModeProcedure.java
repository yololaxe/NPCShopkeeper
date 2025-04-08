package fr.renblood.npcshopkeeper.procedures;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.manager.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.RoadTickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PointDefiningModeProcedure {



    private static final Logger LOGGER = LogManager.getLogger(PointDefiningModeProcedure.class);
    public static final HashMap<ServerPlayer, PointDefiningModeProcedure> activeModes = new HashMap<>();

    private final ServerPlayer player;
    private final Level world;
    private final int maxPoints;
    private final int minTimer;
    private final int maxTimer;
    private final List<BlockPos> positions;
    private final String category;

    // Constructeur
    private final String name;

    public PointDefiningModeProcedure(ServerPlayer player, Level world, int maxPoints, int minTimer, int maxTimer, String category, String name) {
        this.player = player;
        this.world = world;
        this.maxPoints = maxPoints;
        this.minTimer = minTimer;
        this.maxTimer = maxTimer;
        this.positions = new ArrayList<>();
        this.category = category;
        this.name = name;
    }

    // Démarrer le mode
    public static void start(ServerPlayer player, Level world, int maxPoints, int minTimer, int maxTimer, String category, String name) {
        PointDefiningModeProcedure mode = new PointDefiningModeProcedure(player, world, maxPoints, minTimer, maxTimer, category, name);
        activeModes.put(player, mode);
        player.displayClientMessage(Component.literal("Vous êtes en mode définir des points. Placez vos points. (/define)"), false);
    }




    // Arrêter le mode
    public static void stop(ServerPlayer player) {
        activeModes.remove(player);
    }

    // Vérifier si un joueur est en mode
    public static boolean isInMode(ServerPlayer player) {
        return activeModes.containsKey(player);
    }

    // Récupérer le mode d'un joueur
    public static PointDefiningModeProcedure getMode(ServerPlayer player) {
        return activeModes.get(player);
    }

    // Ajouter un point
    public void placePoint(BlockPos pos) {
        if (positions.size() >= maxPoints) {
            player.displayClientMessage(Component.literal("Vous avez déjà placé le nombre maximal de points."), false);
            return;
        }

        BlockPos point = pos.above(); // Ajouter y+1
        positions.add(point);
        player.displayClientMessage(Component.literal("Point placé à : " + point.toShortString()), false);
    }

    // Retirer le dernier point
    public void removeLastPoint() {
        if (positions.isEmpty()) {
            player.displayClientMessage(Component.literal("Aucun point à retirer."), false);
            return;
        }

        BlockPos lastPoint = positions.remove(positions.size() - 1);
        player.displayClientMessage(Component.literal("Point retiré : " + lastPoint.toShortString()), false);
    }

    // Annuler le mode
    public void cancel() {
        positions.clear();
        player.displayClientMessage(Component.literal("Mode définir des points annulé."), false);
        stop(player);
    }

    // Terminer et enregistrer la route
    public void finish() {
        LOGGER.info("Début de la procédure finish pour le joueur : " + player.getName().getString());

        if (positions.size() != maxPoints) {
            LOGGER.warn("Points insuffisants : " + positions.size() + "/" + maxPoints);
            player.displayClientMessage(Component.literal("Vous n'avez pas placé tous les points requis. (" + positions.size() + "/" + maxPoints + ")"), false);
            return;
        }

        String id = CommercialRoad.generateUniqueId();
        LOGGER.info("ID de la route généré : " + id);

        CommercialRoad newRoad = new CommercialRoad(id, name, category, new ArrayList<>(positions), new ArrayList<>(), minTimer, maxTimer);

        LOGGER.info("Nouvelle route construite : " + name + ", catégorie : " + category);

        Npcshopkeeper.COMMERCIAL_ROADS.add(newRoad);
        LOGGER.info("Route ajoutée à la liste globale");

        JsonTradeFileManager.saveRoadToFile(newRoad);
        LOGGER.info("Route sauvegardée dans le fichier JSON");

//        NpcSpawnerManager.trySpawnNpcForRoad(player.getServer().overworld(), newRoad); // Ajouter ce log !
//        LOGGER.info("Tentative de spawn immédiat du premier PNJ");

        LOGGER.info("Lancement du spawn différé via RoadTickScheduler");
        RoadTickScheduler.registerRoad(newRoad);

        player.displayClientMessage(Component.literal("Route commerciale enregistrée avec succès : " + newRoad.getName()), false);

        stop(player);
        LOGGER.info("Fin de la procédure finish pour le joueur : " + player.getName().getString());
    }


    // Mettre à jour les particules
    public void updateParticles() {
        if (!(world instanceof ServerLevel serverWorld)) return;

        for (BlockPos pos : positions) {
            serverWorld.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    5,
                    0.1,
                    0.1,
                    0.1,
                    0
            );
        }
    }
}
