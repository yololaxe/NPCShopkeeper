package fr.renblood.npcshopkeeper.ambience;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Query loaded entity sections near the listener instead of every rendered entity. */
public final class NpcAmbientSelection {
    private NpcAmbientSelection() {}
    public static List<Mob> nearest(Level level, LivingEntity listener, double radius, Predicate<Mob> audible) {
        var candidates = level.getEntitiesOfClass(Mob.class, listener.getBoundingBox().inflate(radius),
                mob -> mob.distanceToSqr(listener) <= radius * radius && audible.test(mob));
        var nearest = new ArrayList<Mob>(5);
        Comparator<Mob> distance = Comparator.comparingDouble(mob -> mob.distanceToSqr(listener));
        for (Mob candidate : candidates) {
            nearest.add(candidate);
            nearest.sort(distance);
            if (nearest.size() > 4) nearest.remove(4);
        }
        return nearest;
    }
}
