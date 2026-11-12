package fr.renblood.npcshopkeeper.services;

import com.google.gson.*;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=Npcshopkeeper.MODID)
public final class VoyageService {
    private static final Set<UUID> PAYING=new HashSet<>();
    private static final Set<UUID> WARNED=new HashSet<>();
    private static void countdown(ServerPlayer p,long time){fr.renblood.npcshopkeeper.network.PacketHandler.sendToPlayer(new fr.renblood.npcshopkeeper.network.SyncDepartureTimePacket(time),p);}
    public static void showTicket(ServerPlayer p){
        JsonObject ticket=VoyageData.get(p.getServer()).tickets.getAsJsonObject(p.getStringUUID());
        if(ticket==null){p.sendSystemMessage(Component.literal("Aucun billet actif."));return;}
        String state=ticket.get("state").getAsString();
        if(state.equals("PAID_PENDING")||state.equals("REFUND_PENDING")){
            String key=state.equals("REFUND_PENDING")?UUID.nameUUIDFromBytes(("refund:"+ticket.get("id").getAsString()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString():ticket.get("id").getAsString();
            WorkshopServer.async(p,()->(JsonObject)Class.forName("fr.renblood.medievalcoins.bank.NpcCommerceGateway").getMethod("ticketStatus",ServerPlayer.class,String.class).invoke(null,p,key),r->{
                if(r.has("state")&&r.get("state").getAsString().equals("CONFIRMED")){
                    ticket.addProperty("state",state.equals("REFUND_PENDING")?"REFUNDED":"BOOKED");VoyageData.get(p.getServer()).persist(p.getServer());
                    p.sendSystemMessage(Component.literal("Billet rapproché : "+ticket.get("state").getAsString()));
                }else p.sendSystemMessage(Component.literal("Reçu encore en attente. Utilise /mc economie reprendre ou contacte l’équipe."));
            });return;
        }
        String origin=ticket.get("origin").getAsString();
        if(near(p,origin)!=null){open(p,origin,false);return;}
        p.sendSystemMessage(Component.literal("Billet pour "+ticket.get("destination").getAsString()+" · "+state+" · départ du port "+origin));
    }
    private static Port near(ServerPlayer p,String name){return PortManager.getPort(name).filter(port->port.getDimension().equals(p.level().dimension().location().toString())&&p.distanceToSqr(port.getPos().getX()+.5,port.getPos().getY(),port.getPos().getZ()+.5)<=100).orElse(null);}
    public static boolean onDuty(Port p,long now){return p.isEnabled()&&VoyageRules.open(now,p.getOpens(),p.getCloses());}
    public static void open(ServerPlayer p,String name,boolean edit){Port port=near(p,name);if(port==null)return;if(edit&&!WorkshopServer.admin(p))return;
        JsonObject data=snapshot(p,port);WorkshopServer.reply(p,edit?"HARBOR_EDIT":"VOYAGE",data);
    }
    private static JsonObject snapshot(ServerPlayer p,Port port){
        JsonObject data=new JsonObject();data.add("origin",port.toJson());data.addProperty("now",p.serverLevel().getDayTime());data.addProperty("open",onDuty(port,p.serverLevel().getDayTime()));data.addProperty("departure",VoyageRules.next(p.serverLevel().getDayTime(),port.getCloses(),port.getDeparture()));
        JsonArray dests=new JsonArray();for(Port dest:PortManager.getAllPorts()){if(dest.getName().equals(port.getName()))continue;JsonObject row=dest.toJson();boolean served=dest.isEnabled()&&port.serves(dest.getName())&&dest.getDimension().equals(port.getDimension());row.addProperty("served",served);row.addProperty("price",Long.toString(cost(port,dest)));row.addProperty("fixedFare",port.getFixedFare(dest.getName()));dests.add(row);}data.add("destinations",dests);
        JsonObject ticket=VoyageData.get(p.getServer()).tickets.getAsJsonObject(p.getStringUUID());if(ticket!=null)data.add("ticket",ticket.deepCopy());return data;
    }
    private static boolean connectionOpen(Port origin,String destination){return PortManager.getPort(destination).filter(p->p.isEnabled()&&origin.serves(destination)&&p.getDimension().equals(origin.getDimension())).isPresent();}
    private static long cost(Port from,Port to){return VoyageRules.price(Math.sqrt(from.getPos().distSqr(to.getPos())),Math.max(1,PortManager.getBlocksPerIron()),from.getFixedFare(to.getName()));}
    public static void handle(ServerPlayer p,String action,JsonObject body)throws Exception{
        String origin=body.has("origin")?body.get("origin").getAsString():"";Port port=near(p,origin);
        if(action.equals("VOYAGE_STATUS")){open(p,origin,false);return;}
        if(port==null){WorkshopServer.error(p,"Rejoins le quai de départ.");return;}
        VoyageData saved=VoyageData.get(p.getServer());String uuid=p.getStringUUID();JsonObject existing=saved.tickets.getAsJsonObject(uuid);
        switch(action){
            case "HARBOR_SAVE" -> {
                if(!WorkshopServer.admin(p))return;
                Port updated=Port.fromJson(body.getAsJsonObject("port"));
                if(!updated.getName().equals(port.getName())||!updated.getDimension().equals(port.getDimension())||!updated.getPos().equals(port.getPos()))throw new IllegalArgumentException("Le quai doit être déplacé en jeu.");
                if(updated.getRevision()!=port.getRevision()){WorkshopServer.error(p,"Ce port a été modifié ailleurs. Recharge-le avant d’enregistrer.");return;}
                updated.validate();updated.setRevision(port.getRevision()+1);PortManager.addPort(updated);open(p,origin,true);
            }
            case "VOYAGE_BOOK" -> {
                if(!onDuty(port,p.serverLevel().getDayTime())){WorkshopServer.error(p,"La billetterie est fermée.");return;}
                if(existing!=null&&!Set.of("ARRIVED","REFUNDED").contains(existing.get("state").getAsString())){WorkshopServer.error(p,"Tu possèdes déjà un billet. Reprends-le ou annule-le.");return;}
                Port destination=PortManager.getPort(body.get("destination").getAsString()).orElseThrow();
                if(!connectionOpen(port,destination.getName())||destination.getName().equals(origin)){WorkshopServer.error(p,"Cette destination n’est pas desservie actuellement.");return;}
                long price=cost(port,destination);
                if(!Long.toString(price).equals(body.get("price").getAsString())){WorkshopServer.error(p,"Le tarif a changé. Actualise les destinations.");return;}
                if(!PAYING.add(p.getUUID()))return;
                String key=UUID.randomUUID().toString();JsonObject ticket=new JsonObject();ticket.addProperty("id",key);ticket.addProperty("origin",origin);ticket.addProperty("destination",destination.getName());ticket.add("arrival",destination.toJson());ticket.addProperty("price",Long.toString(price));ticket.addProperty("departure",VoyageRules.next(p.serverLevel().getDayTime(),port.getCloses(),port.getDeparture()));ticket.addProperty("state","PAID_PENDING");ticket.addProperty("close_at",ticket.get("departure").getAsLong()-(port.getDeparture()-port.getCloses()));
                try { EconomyBridge.ticket(p,key,origin,price,()->near(p,origin)!=null&&onDuty(port,p.serverLevel().getDayTime())&&saved.tickets.getAsJsonObject(uuid)==existing,
                    ()->saved.tickets.add(uuid,ticket),()->saved.persist(p.getServer()),false,"").whenComplete((receipt,error)->p.getServer().execute(()->{
                        PAYING.remove(p.getUUID());if(error==null&&receipt.get("state").getAsString().equals("CONFIRMED")){ticket.addProperty("state","BOOKED");ticket.addProperty("receipt",receipt.get("operation_id").getAsString());saved.persist(p.getServer());countdown(p,ticket.get("departure").getAsLong());open(p,origin,false);}else WorkshopServer.error(p,"Paiement non confirmé. Consulte le reçu avant de reprendre un billet.");
                    })); } catch(Exception failure){PAYING.remove(p.getUUID());throw failure;}
            }
            case "VOYAGE_REBOOK" -> {
                if(existing==null||!existing.get("state").getAsString().equals("MISSED")){WorkshopServer.error(p,"Ce billet ne peut pas être reporté dans son état actuel.");return;}
                if(!existing.get("origin").getAsString().equals(origin)||!onDuty(port,p.serverLevel().getDayTime())){WorkshopServer.error(p,"Reviens au port d’origine pendant l’ouverture pour reporter ton billet.");return;}
                if(!connectionOpen(port,existing.get("destination").getAsString())){WorkshopServer.error(p,"Cette liaison est suspendue ; tu peux demander le remboursement.");return;}
                existing.addProperty("departure",VoyageRules.next(p.serverLevel().getDayTime(),port.getCloses(),port.getDeparture()));existing.addProperty("state","BOOKED");existing.addProperty("close_at",existing.get("departure").getAsLong()-(port.getDeparture()-port.getCloses()));saved.persist(p.getServer());WARNED.remove(p.getUUID());countdown(p,existing.get("departure").getAsLong());open(p,origin,false);
            }
            case "VOYAGE_CANCEL" -> {
                if(existing==null||!existing.get("origin").getAsString().equals(origin)||!Set.of("BOOKED","MISSED").contains(existing.get("state").getAsString())){WorkshopServer.error(p,"Ce billet ne peut pas être remboursé dans son état actuel. Consulte son reçu.");return;}
                if(existing.get("state").getAsString().equals("BOOKED")&&p.serverLevel().getDayTime()>=(existing.has("close_at")?existing.get("close_at").getAsLong():existing.get("departure").getAsLong()-(port.getDeparture()-port.getCloses()))){WorkshopServer.error(p,"Embarquement clos. Le billet sera reportable après le départ.");return;}
                if(!PAYING.add(p.getUUID()))return;String previous=existing.get("state").getAsString();String key=UUID.nameUUIDFromBytes(("refund:"+existing.get("id").getAsString()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
                try { EconomyBridge.ticket(p,key,origin,existing.get("price").getAsLong(),()->saved.tickets.getAsJsonObject(uuid)==existing&&existing.get("state").getAsString().equals(previous),
                    ()->existing.addProperty("state","REFUND_PENDING"),()->saved.persist(p.getServer()),true,existing.get("id").getAsString()).whenComplete((receipt,error)->p.getServer().execute(()->{PAYING.remove(p.getUUID());if(error==null&&receipt.get("state").getAsString().equals("CONFIRMED")){existing.addProperty("state","REFUNDED");saved.persist(p.getServer());countdown(p,-1);open(p,origin,false);}else WorkshopServer.error(p,"Remboursement non confirmé : consulte le reçu.");})); } catch(Exception failure){PAYING.remove(p.getUUID());throw failure;}
            }
            case "VOYAGE_TEST" -> {if(!WorkshopServer.admin(p))return;if(existing==null||!existing.get("state").getAsString().equals("BOOKED"))return;existing.addProperty("departure",p.serverLevel().getDayTime());saved.persist(p.getServer());}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||e.getServer().getTickCount()%20!=0)return;var server=e.getServer();long now=server.overworld().getDayTime();
        for(var npc:NpcEntityIndex.all()){if(!npc.isCaptain())continue;PortManager.getPort(npc.getPortName()).ifPresent(port->{npc.setInvisible(!onDuty(port,now));npc.setNoAi(true);});}
        VoyageData data=VoyageData.get(server);boolean changed=false;int remaining=4;
        for(var entry:new ArrayList<>(data.tickets.entrySet())){
            JsonObject ticket=entry.getValue().getAsJsonObject();String state=ticket.get("state").getAsString();ServerPlayer p;
            try{p=server.getPlayerList().getPlayer(UUID.fromString(entry.getKey()));}catch(Exception bad){continue;}
            if(state.equals("BOOKED")){
                long until=ticket.get("departure").getAsLong()-now;
                if(p!=null&&until>0&&until<=1200&&WARNED.add(p.getUUID()))p.sendSystemMessage(Component.literal("Ton bateau part bientôt. Rejoins le quai de "+ticket.get("origin").getAsString()+".").withStyle(net.minecraft.ChatFormatting.GOLD));
                if(until>0)continue;
                Port origin=PortManager.getPort(ticket.get("origin").getAsString()).orElse(null);
                if(p==null||origin==null||!origin.isEnabled()||!connectionOpen(origin,ticket.get("destination").getAsString())||near(p,origin.getName())==null||p.isPassenger()||p.isSleeping()||!p.isAlive()){
                    ticket.addProperty("state","MISSED");changed=true;if(p!=null)p.sendSystemMessage(Component.literal("Départ manqué : ton billet reste reportable gratuitement auprès du capitaine."));continue;
                }
                ticket.addProperty("state","SAILING");ticket.addProperty("board_x",p.getX());ticket.addProperty("board_y",p.getY());ticket.addProperty("board_z",p.getZ());ticket.addProperty("arrival_tick",server.overworld().getGameTime()+200);ticket.addProperty("departure_yaw",p.getYRot());p.setDeltaMovement(0,0,0);p.closeContainer();countdown(p,-1);JsonObject scene=new JsonObject();scene.addProperty("destination",ticket.get("destination").getAsString());scene.addProperty("duration",200);WorkshopServer.reply(p,"SAILING",scene);changed=true;
            }else if((state.equals("SAILING")||state.equals("ARRIVAL_WAIT"))&&p!=null&&p.isAlive()&&remaining-->0&&server.overworld().getGameTime()>=ticket.get("arrival_tick").getAsLong()){
                Port destination=Port.fromJson(ticket.getAsJsonObject("arrival"));ServerLevel level=server.getLevel(ResourceKey.create(Registries.DIMENSION,new ResourceLocation(destination.getDimension())));
                if(level==null){ticket.addProperty("state","ARRIVAL_WAIT");ticket.addProperty("arrival_tick",server.overworld().getGameTime()+600);changed=true;continue;}
                BlockPos at=destination.getPos();level.getChunk(at);
                if(!level.getBlockState(at).getCollisionShape(level,at).isEmpty()||!level.getBlockState(at.above()).getCollisionShape(level,at.above()).isEmpty()||!level.getFluidState(at).isEmpty()||level.getBlockState(at.below()).getCollisionShape(level,at.below()).isEmpty()){
                    ticket.addProperty("state","ARRIVAL_WAIT");ticket.addProperty("arrival_tick",server.overworld().getGameTime()+600);WorkshopServer.reply(p,"ARRIVED",new JsonObject());p.sendSystemMessage(Component.literal("Quai d’arrivée indisponible : ton passage est conservé. Contacte l’équipe."));changed=true;continue;
                }
                p.teleportTo(level,at.getX()+.5,at.getY(),at.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;ticket.addProperty("state","ARRIVED");WorkshopServer.reply(p,"ARRIVED",new JsonObject());p.sendSystemMessage(Component.literal("Bienvenue à "+destination.getCityName()+".").withStyle(net.minecraft.ChatFormatting.GOLD));changed=true;
            }
        }
        if(changed)data.persist(server);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(!(e.getEntity() instanceof ServerPlayer p))return;countdown(p,-1);JsonObject ticket=VoyageData.get(p.getServer()).tickets.getAsJsonObject(p.getStringUUID());if(ticket==null)return;String state=ticket.get("state").getAsString();
        if(state.equals("BOOKED"))countdown(p,ticket.get("departure").getAsLong());
        if(state.equals("BOOKED")||state.equals("MISSED"))p.sendSystemMessage(Component.literal("Billet conservé pour "+ticket.get("destination").getAsString()+". Consulte le capitaine de "+ticket.get("origin").getAsString()+"."));
        if(state.equals("PAID_PENDING")||state.equals("REFUND_PENDING"))p.sendSystemMessage(Component.literal("Un reçu de traversée doit être rapproché. /mc economie reprendre puis /ns harbor ticket."));
    }
    @SubscribeEvent public static void freeze(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p))return;
        JsonObject ticket=VoyageData.get(p.getServer()).tickets.getAsJsonObject(p.getStringUUID());
        if(ticket==null||!"SAILING".equals(ticket.get("state").getAsString())||!ticket.has("board_x"))return;
        double x=ticket.get("board_x").getAsDouble(),y=ticket.get("board_y").getAsDouble(),z=ticket.get("board_z").getAsDouble();
        p.setDeltaMovement(0,0,0);p.fallDistance=0;
        if(p.distanceToSqr(x,y,z)>.04)p.connection.teleport(x,y,z,p.getYRot(),p.getXRot());
    }
    @SubscribeEvent public static void protect(net.minecraftforge.event.entity.living.LivingAttackEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        JsonObject ticket=VoyageData.get(p.getServer()).tickets.getAsJsonObject(p.getStringUUID());
        if(ticket!=null&&"SAILING".equals(ticket.get("state").getAsString()))e.setCanceled(true);
    }
    @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){PAYING.clear();WARNED.clear();}
}
