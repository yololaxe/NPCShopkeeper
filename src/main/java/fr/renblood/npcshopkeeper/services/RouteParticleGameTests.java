package fr.renblood.npcshopkeeper.services;
import fr.renblood.npcshopkeeper.procedures.route.PointDefiningModeProcedure;
import fr.renblood.npcshopkeeper.world.inventory.ParticleUpdateHandler;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("npcshopkeeper")
@PrefixGameTestTemplate(false)
public final class RouteParticleGameTests {
    private static ServerPlayer player(GameTestHelper h,AtomicInteger packets){
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"marker-fixture"));
        p.connection=new ServerGamePacketListenerImpl(h.getLevel().getServer(),new Connection(PacketFlow.SERVERBOUND),p){
            @Override public void send(Packet<?> packet){if(packet instanceof ClientboundLevelParticlesPacket)packets.incrementAndGet();}
        };
        var at=h.absolutePos(net.minecraft.core.BlockPos.ZERO);p.setPos(at.getX()+.5,at.getY()+1,at.getZ()+.5);return p;
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void routeMarkersOnlyReachTheirEditorAndNearbyPoints(GameTestHelper h){
        AtomicInteger packets=new AtomicInteger();var p=player(h,packets);
        try{
            PointDefiningModeProcedure.start(p,h.getLevel(),2,10,20,"fixture","fixture",CommercialRoad.RoadType.TRADE);
            var mode=PointDefiningModeProcedure.getMode(p);mode.placePoint(p.blockPosition().below());mode.placePoint(p.blockPosition().offset(64,-1,0));
            mode.updateParticles();h.assertTrue(packets.get()==1,"One near point creates one targeted particle packet; far points send none");
        }finally{PointDefiningModeProcedure.stop(p);}h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void logoutClearsOnlyTheDepartingEditorsMode(GameTestHelper h){
        var a=player(h,new AtomicInteger());var b=player(h,new AtomicInteger());
        try{
            PointDefiningModeProcedure.start(a,h.getLevel(),1,10,20,"fixture","a",CommercialRoad.RoadType.TRADE);
            PointDefiningModeProcedure.start(b,h.getLevel(),1,10,20,"fixture","b",CommercialRoad.RoadType.TRADE);
            ParticleUpdateHandler.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(a));
            h.assertTrue(!PointDefiningModeProcedure.isInMode(a)&&PointDefiningModeProcedure.isInMode(b),"Logout releases only the departing player's saved mode");
        }finally{PointDefiningModeProcedure.stop(a);PointDefiningModeProcedure.stop(b);}h.succeed();
    }
}
