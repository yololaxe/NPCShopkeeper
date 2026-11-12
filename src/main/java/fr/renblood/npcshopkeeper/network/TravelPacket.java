package fr.renblood.npcshopkeeper.network;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.services.VoyageService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
/** Compatibility packet: no legacy payment or transient reservation is performed. */
public final class TravelPacket {
    private final String destination;private final int estimate;
    public TravelPacket(String destination,int estimate){this.destination=destination;this.estimate=estimate;}
    public TravelPacket(FriendlyByteBuf b){this(b.readUtf(),b.readInt());}
    public void toBytes(FriendlyByteBuf b){b.writeUtf(destination);b.writeInt(estimate);}
    public void handle(Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->{var p=c.getSender();if(p==null)return;NpcEntityIndex.all().stream().filter(n->n.isCaptain()&&n.level()==p.level()&&n.distanceToSqr(p)<100).findFirst().ifPresent(n->VoyageService.open(p,n.getPortName(),false));});c.setPacketHandled(true);}
}
