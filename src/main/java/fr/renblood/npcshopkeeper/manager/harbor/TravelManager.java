package fr.renblood.npcshopkeeper.manager.harbor;

import fr.renblood.npcshopkeeper.data.harbor.Port;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class TravelManager {
    // Map<PortName, List<Passenger>>
    private static final Map<String, List<Passenger>> pendingDepartures = new HashMap<>();
    
    // Map<PlayerUUID, DepartureTime> pour le HUD client (sera sync)
    private static final Map<UUID, Long> playerDepartureTimes = new HashMap<>();

    public static void registerPassenger(ServerPlayer player, String fromPortName, Port destination) {
        pendingDepartures.computeIfAbsent(fromPortName, k -> new ArrayList<>()).add(new Passenger(player.getUUID(), destination));
        // On stocke aussi l'heure de départ prévue pour l'affichage HUD (sera géré par le tick du capitaine)
    }

    public static List<Passenger> getPassengers(String portName) {
        return pendingDepartures.getOrDefault(portName, Collections.emptyList());
    }

    public static void clearPassengers(String portName) {
        pendingDepartures.remove(portName);
    }

    public static class Passenger {
        public final UUID playerUUID;
        public final Port destination;

        public Passenger(UUID playerUUID, Port destination) {
            this.playerUUID = playerUUID;
            this.destination = destination;
        }
    }
    
    // Méthodes pour le HUD client
    public static void setDepartureTime(UUID playerUUID, long time) {
        playerDepartureTimes.put(playerUUID, time);
    }
    
    public static Long getDepartureTime(UUID playerUUID) {
        return playerDepartureTimes.get(playerUUID);
    }
    
    public static void removeDepartureTime(UUID playerUUID) {
        playerDepartureTimes.remove(playerUUID);
    }
}
