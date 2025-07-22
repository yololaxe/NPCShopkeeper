package fr.renblood.npcshopkeeper.procedures.route;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.road.RoadTickScheduler;
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
    private final int maxPoints, minTimer, maxTimer;
    private final List<BlockPos> positions = new ArrayList<>();
    private final String category, name;

    // Constructeur
    private PointDefiningModeProcedure(ServerPlayer player, Level world,
                                        int maxPoints, int minTimer, int maxTimer,
                                        String category, String name) {
        this.player = player;
        this.world = world;
        this.maxPoints = maxPoints;
        this.minTimer = minTimer;
        this.maxTimer = maxTimer;
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
        if (positions.size()>=maxPoints) {
            player.displayClientMessage(Component.literal("Max points atteints"),false);
            return;
        }
        positions.add(pos.above());
        player.displayClientMessage(Component.literal("Point ajouté : "+pos.above()),false);
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
        if (positions.size()!=maxPoints) {
            player.displayClientMessage(Component.literal("Points insuffisants"),false);
            return;
        }
        var id = CommercialRoad.generateUniqueId();
        var road = new CommercialRoad(id, name, category,
                new ArrayList<>(positions),
                new ArrayList<>(),
                minTimer, maxTimer);
        Npcshopkeeper.COMMERCIAL_ROADS.add(road);

        // Sauvegarde
        var repo = new fr.renblood.npcshopkeeper.data.io.JsonRepository<>(
                java.nio.file.Paths.get(fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json,(ServerLevel)world),
                CommercialRoad::toJson
        );
        NpcSpawnerManager.trySpawnNpcForRoad((ServerLevel)world, road);

        // puis scheduler pour les suivants
        RoadTickScheduler.registerRoad(road);

        player.displayClientMessage(Component.literal("Route créée : "+name),false);
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
