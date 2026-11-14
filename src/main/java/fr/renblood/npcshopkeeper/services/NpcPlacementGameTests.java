package fr.renblood.npcshopkeeper.services;

import com.google.gson.Gson;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcSpawnManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;

@GameTestHolder("npcshopkeeper")
@PrefixGameTestTemplate(false)
public final class NpcPlacementGameTests {
    private static NpcSpawn spawn(String id,long revision,boolean active) {
        return new Gson().fromJson("{\"spawn_id\":\""+id+"\",\"npc_id\":\"placement-fixture\",\"npc_name\":\"Placement fixture\",\"npc_type\":\"QUEST\",\"npc_skin\":\"keep\",\"world\":\"minecraft:overworld\",\"x\":10.5,\"y\":80,\"z\":20.5,\"active\":"+active+",\"spawn_rule\":\"STATIC\",\"meta\":{\"placement_revision\":"+revision+"}}",NpcSpawn.class);
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void deletedSpawnCannotReturnFromStaleCache(GameTestHelper h) {
        var data=new NpcPlacementSavedData();data.record(spawn("deleted",2,false));
        var restored=NpcPlacementSavedData.load(data.save(new CompoundTag()));
        h.assertTrue(!restored.reconcile(spawn("deleted",1,true)).active,"Deleted spawn stays inactive after NBT reload and stale HTTP response");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void movedSpawnKeepsNewPositionAndFreshSkin(GameTestHelper h) {
        var data=new NpcPlacementSavedData();var latest=spawn("move",2,true);latest.x=100.5;latest.world="minecraft:the_nether";data.record(latest);
        var old=spawn("move",1,true);old.npc_skin="new-skin";var merged=data.reconcile(old);
        h.assertTrue(merged.x==100.5&&merged.world.equals("minecraft:the_nether"),"Stale placement must not move NPC back");
        h.assertTrue(merged.npc_skin.equals("new-skin")&&old.x==10.5,"Skin updates preserved; shared API cache object unmodified");
        var newer=spawn("move",3,true);newer.x=200.5;
        h.assertTrue(data.reconcile(newer).x==200.5&&data.find("move").x==200.5,"Newer server edit supersedes local guard");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=50)
    public static void confirmedDeleteRejectsSavedEntity(GameTestHelper h) {
        var level=h.getLevel();String id="fixture-delete-"+java.util.UUID.randomUUID();
        var state=spawn(id,1,false);state.world=level.dimension().location().toString();
        NpcPlacementSavedData.get(level.getServer()).record(state);
        var stale=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),level);stale.setApiNpcSpawnId(id);stale.setPos(10.5,80,20.5);
        h.assertTrue(GlobalNpcSpawnManager.shouldRejectJoiningEntity(level,stale),"Serialized entity of deleted spawn must not rejoin on chunk load");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void oldEntityLeavingCannotEraseReplacementIndex(GameTestHelper h) {
        String id="index-fixture-"+java.util.UUID.randomUUID();var old=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),h.getLevel());
        var replacement=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),h.getLevel());replacement.setUUID(old.getUUID());
        old.getPersistentData().putString("ApiNpcSpawnId",id);replacement.getPersistentData().putString("ApiNpcSpawnId",id);
        NpcEntityIndex.add(old);NpcEntityIndex.add(replacement);NpcEntityIndex.remove(old);
        h.assertTrue(NpcEntityIndex.getBySpawnId(id)==replacement,"Old entity departure must not erase the new entity's stable spawn lookup");
        NpcEntityIndex.remove(replacement);h.assertTrue(NpcEntityIndex.getBySpawnId(id)==null,"Actual replacement removal cleans its index");h.succeed();
    }
}
