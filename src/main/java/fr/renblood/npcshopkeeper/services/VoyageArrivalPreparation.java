package fr.renblood.npcshopkeeper.services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import java.util.*;
import java.util.function.*;

/** Owns a bounded set of chunk tickets; never waits on generation or joins a future. */
final class VoyageArrivalPreparation implements AutoCloseable {
    enum State { WORKING, READY, FAILED }
    private static final TicketType<String> TICKET=TicketType.create("renblood_voyage",String::compareTo);
    private final List<ChunkPos> chunks;
    private final Set<ChunkPos> owned=new LinkedHashSet<>();
    private final Consumer<ChunkPos> request,release;
    private final Predicate<ChunkPos> ready;
    private final LongSupplier clock;
    private final long deadline;
    private State state=State.WORKING;
    private boolean closed;
    static VoyageArrivalPreparation create(ServerLevel level,BlockPos at,String ticketId) {
        List<ChunkPos> chunks=new ArrayList<>();int x=at.getX()>>4,z=at.getZ()>>4;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)chunks.add(new ChunkPos(x+dx,z+dz));
        String key=ticketId+":"+UUID.randomUUID();
        return new VoyageArrivalPreparation(chunks,
                p->level.getChunkSource().addRegionTicket(TICKET,p,0,key),
                p->level.getChunkSource().getChunkNow(p.x,p.z)!=null,
                p->level.getChunkSource().removeRegionTicket(TICKET,p,0,key),System::nanoTime,90_000_000_000L);
    }
    VoyageArrivalPreparation(List<ChunkPos> chunks,Consumer<ChunkPos> request,Predicate<ChunkPos> ready,
                             Consumer<ChunkPos> release,LongSupplier clock,long timeout) {
        this.chunks=List.copyOf(new LinkedHashSet<>(chunks));
        if(this.chunks.isEmpty()||this.chunks.size()>9)throw new IllegalArgumentException("Arrival preparation must contain 1..9 chunks");
        this.request=request;this.ready=ready;this.release=release;this.clock=clock;deadline=clock.getAsLong()+timeout;
    }
    State tick() {
        if(closed)return State.FAILED;
        if(clock.getAsLong()-deadline>=0){state=State.FAILED;close();return state;}
        try {
            if(owned.size()<chunks.size()) {ChunkPos next=chunks.get(owned.size());owned.add(next);request.accept(next);}
            state=owned.size()==chunks.size()&&chunks.stream().allMatch(ready)?State.READY:State.WORKING;
        }catch(RuntimeException failure){state=State.FAILED;close();}
        return state;
    }
    State state(){return state;}
    @Override public void close() {
        if(closed)return;closed=true;
        RuntimeException problem=null;
        for(ChunkPos chunk:owned)try{release.accept(chunk);}catch(RuntimeException failure){if(problem==null)problem=failure;}
        // Try every release even if one chunk source has failed during shutdown.
        if(problem!=null)org.apache.logging.log4j.LogManager.getLogger(VoyageArrivalPreparation.class).warn("Libération d'un ticket d'arrivée différée",problem);
    }
}
