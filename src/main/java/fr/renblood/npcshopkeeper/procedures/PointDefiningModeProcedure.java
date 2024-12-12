package fr.renblood.npcshopkeeper.procedures;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PointDefiningModeProcedure {
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
        if (positions.size() != maxPoints) {
            player.displayClientMessage(Component.literal("Vous n'avez pas placé tous les points requis. (" + positions.size() + "/" + maxPoints + ")"), false);
            return;
        }

        // Créer une nouvelle route commerciale
        String id = CommercialRoad.generateUniqueId();
        CommercialRoad newRoad = new CommercialRoad(
                id,
                name,
                category, // Catégorie par défaut (modifiable)
                new ArrayList<>(positions), // Copier les positions
                new ArrayList<>(), // Entités vides
                minTimer,
                maxTimer
        );

        // Ajouter à la liste globale
        Npcshopkeeper.COMMERCIAL_ROADS.add(newRoad);

        // Sauvegarder dans le fichier JSON
        JsonTradeFileManager.saveRoadToFile(newRoad);

        // Message de confirmation
        player.displayClientMessage(Component.literal("Route commerciale enregistrée avec succès : " + newRoad.getName()), false);

        // Arrêter le mode
        stop(player);
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
