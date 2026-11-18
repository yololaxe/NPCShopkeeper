package fr.renblood.npcshopkeeper.services;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;

@GameTestHolder("npcshopkeeper") @PrefixGameTestTemplate(false)
public final class VoyageArrivalGameTests {
    @GameTest(template="services_empty") public static void arrivalRequestsAreIncrementalAndWaitForReadiness(GameTestHelper h) {
        var requested=new HashSet<ChunkPos>();var released=new HashSet<ChunkPos>();var loaded=new HashSet<ChunkPos>();
        var chunks=List.of(new ChunkPos(1,1),new ChunkPos(2,1),new ChunkPos(3,1));
        var job=new VoyageArrivalPreparation(chunks,requested::add,loaded::contains,released::add,()->0,100);
        h.assertTrue(job.tick()==VoyageArrivalPreparation.State.WORKING&&requested.size()==1,"One request this tick without waiting on generation");
        job.tick();job.tick();h.assertTrue(requested.size()==3&&released.isEmpty(),"Owned tickets held until safe arrival");
        loaded.addAll(chunks);h.assertTrue(job.tick()==VoyageArrivalPreparation.State.READY,"Arrival waits for every required chunk");
        job.close();job.close();h.assertTrue(released.equals(requested),"Every ticket released exactly once");h.succeed();
    }
    @GameTest(template="services_empty") public static void arrivalTimeoutAndPartialFailureReleaseTickets(GameTestHelper h) {
        var clock=new AtomicLong();var released=new AtomicInteger();
        var job=new VoyageArrivalPreparation(List.of(new ChunkPos(1,1)),p->{},p->false,p->released.incrementAndGet(),clock::get,100);
        job.tick();clock.set(101);h.assertTrue(job.tick()==VoyageArrivalPreparation.State.FAILED&&released.get()==1,"Timeout releases terrain ownership");
        var failed=new VoyageArrivalPreparation(List.of(new ChunkPos(2,2)),p->{throw new IllegalStateException("fixture");},p->false,p->released.incrementAndGet(),clock::get,100);
        h.assertTrue(failed.tick()==VoyageArrivalPreparation.State.FAILED&&released.get()==2,"Partially created requests are also released");h.succeed();
    }
    @GameTest(template="services_empty") public static void closedArrivalCannotBecomeReadyAgain(GameTestHelper h) {
        var requests=new AtomicInteger();var job=new VoyageArrivalPreparation(List.of(new ChunkPos(0,0)),p->requests.incrementAndGet(),p->true,p->{},()->0,100);
        job.close();h.assertTrue(job.tick()==VoyageArrivalPreparation.State.FAILED&&requests.get()==0,"Logout/shutdown cancellation cannot acquire new tickets");h.succeed();
    }
    @GameTest(template="services_empty") public static void nativeChunkTicketApiRemainsNonBlocking(GameTestHelper h) {
        var job=VoyageArrivalPreparation.create(h.getLevel(),h.absolutePos(BlockPos.ZERO),"native-fixture");
        try{h.assertTrue(job.tick()==VoyageArrivalPreparation.State.WORKING,"A real region ticket is requested without forcing all nine chunks");}
        finally{job.close();}h.succeed();
    }
    @GameTest(template="services_empty") public static void arrivalAdmissionIsBoundedAndStaleSessionsAreRemoved(GameTestHelper h) {
        var server=h.getLevel().getServer();var saved=VoyageData.get(server);var ids=new ArrayList<String>();
        try {
            for(int i=0;i<5;i++) {
                var player=new ServerPlayer(server,h.getLevel(),new GameProfile(UUID.randomUUID(),"arrival-fixture"));
                var ticket=new JsonObject();ticket.addProperty("id",UUID.randomUUID().toString());ticket.addProperty("state","SAILING");
                saved.tickets.add(player.getStringUUID(),ticket);ids.add(player.getStringUUID());
                VoyageArrivals.prepare(player,ticket,h.getLevel(),h.absolutePos(BlockPos.ZERO));
            }
            h.assertTrue(VoyageArrivals.activeCount()==4,"Fifth passenger does not create a fifth terrain preparation");
            VoyageArrivals.tick(server,p->false);h.assertTrue(VoyageArrivals.activeCount()==0,"Disconnected/replaced sessions release every admission slot");
        }finally{VoyageArrivals.clear();ids.forEach(saved.tickets::remove);}h.succeed();
    }
}
