package fr.renblood.npcshopkeeper.ambience;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("npcshopkeeper")
@PrefixGameTestTemplate(false)
public final class AmbientSelectionGameTests {
    private static Villager spawn(GameTestHelper h, double x, double z) {
        var v=new Villager(EntityType.VILLAGER,h.getLevel());var origin=h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        v.moveTo(origin.getX()+x,origin.getY()+1,origin.getZ()+z);v.setNoAi(true);h.getLevel().addFreshEntity(v);return v;
    }
    @GameTest(template="services_empty")
    public static void selectionKeepsOnlyFourNearestAudibleNpcs(GameTestHelper h) {
        var listener=spawn(h,1,1);var ids=new HashSet<UUID>();
        for(int i=2;i<=7;i++)ids.add(spawn(h,i,1).getUUID());
        var found=NpcAmbientSelection.nearest(h.getLevel(),listener,10,m->ids.contains(m.getUUID()));
        h.assertTrue(found.size()==4&&found.get(3).distanceToSqr(listener)==16,"Select only four nearest after spatial lookup");
        h.assertTrue(found.stream().allMatch(m->m.distanceToSqr(listener)<=16),"Further candidates excluded");h.succeed();
    }
    @GameTest(template="services_empty")
    public static void radiusIsSphericalAndPredicateIsRespected(GameTestHelper h) {
        var listener=spawn(h,1,1);var allowed=spawn(h,4,1);var corner=spawn(h,4,4);var muted=spawn(h,2,1);muted.setSilent(true);
        Set<Mob> candidates=Set.of(allowed,corner,muted);
        var found=NpcAmbientSelection.nearest(h.getLevel(),listener,3,m->candidates.contains(m)&&!m.isSilent());
        h.assertTrue(found.equals(List.of(allowed)),"Cube corner and silent entity never enter the sound budget; boundary included");h.succeed();
    }
}
