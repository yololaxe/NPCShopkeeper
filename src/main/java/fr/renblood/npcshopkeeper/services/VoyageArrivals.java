package fr.renblood.npcshopkeeper.services;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** At most four arrivals prepare terrain at once. Runtime work is rebuilt from persistent tickets. */
final class VoyageArrivals {
    private record Job(ServerPlayer player,JsonObject ticket,ServerLevel level,BlockPos at,VoyageArrivalPreparation terrain) {}
    private static final Map<UUID,Job> JOBS=new LinkedHashMap<>();
    static VoyageArrivalPreparation.State prepare(ServerPlayer p,JsonObject ticket,ServerLevel level,BlockPos at) {
        Job job=JOBS.get(p.getUUID());
        if(job!=null&&(job.player()!=p||job.ticket()!=ticket||job.level()!=level||!job.at().equals(at))) {release(p.getUUID());job=null;}
        if(job==null) {
            if(JOBS.size()>=4)return VoyageArrivalPreparation.State.WORKING;
            job=new Job(p,ticket,level,at.immutable(),VoyageArrivalPreparation.create(level,at,ticket.get("id").getAsString()));
            JOBS.put(p.getUUID(),job);
        }
        return job.terrain().state();
    }
    static void prefetch(ServerPlayer p,JsonObject ticket) {
        var destination=fr.renblood.npcshopkeeper.data.harbor.Port.fromJson(ticket.getAsJsonObject("arrival"));
        ServerLevel level=p.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(destination.getDimension())));
        if(level!=null)prepare(p,ticket,level,destination.getPos());
    }
    static int activeCount(){return JOBS.size();}
    static void tick(MinecraftServer server) {
        tick(server,player->server.getPlayerList().getPlayer(player.getUUID())==player);
    }
    static void tick(MinecraftServer server,java.util.function.Predicate<ServerPlayer> currentSession) {
        var entries=JOBS.entrySet().iterator();
        while(entries.hasNext()) {
            var entry=entries.next();Job job=entry.getValue();
            String state=job.ticket().get("state").getAsString();
            if(!currentSession.test(job.player())||job.player().hasDisconnected()
                    ||job.player().isRemoved()||!job.player().isAlive()
                    ||VoyageData.get(server).tickets.getAsJsonObject(entry.getKey().toString())!=job.ticket()
                    ||!(state.equals("SAILING")||state.equals("ARRIVAL_WAIT"))) {
                job.terrain().close();entries.remove();continue;
            }
            job.terrain().tick();
        }
    }
    static void release(UUID id) {Job job=JOBS.remove(id);if(job!=null)job.terrain().close();}
    static void clear() {JOBS.values().forEach(job->job.terrain().close());JOBS.clear();}
}
