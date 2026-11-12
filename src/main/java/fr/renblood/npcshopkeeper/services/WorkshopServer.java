package fr.renblood.npcshopkeeper.services;

import com.google.gson.*;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class WorkshopServer {
    private static final ExecutorService WORKER=new ThreadPoolExecutor(1,2,30,TimeUnit.SECONDS,new ArrayBlockingQueue<>(64),r->{Thread t=new Thread(r,"NpcServices");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private static final Set<UUID> BUSY=ConcurrentHashMap.newKeySet();
    private static final Map<UUID,Placement> PLACEMENT=new HashMap<>();
    public static void reply(ServerPlayer p,String action,JsonObject body){if(!p.hasDisconnected())PacketHandler.sendToPlayer(new ServiceResponse(action,body.toString()),p);}
    public static void error(ServerPlayer p,String text){JsonObject e=new JsonObject();e.addProperty("error",text);reply(p,"ERROR",e);}
    public static void async(ServerPlayer p,Callable<JsonObject> job,Consumer<JsonObject> done){
        if(!BUSY.add(p.getUUID())){error(p,"Une demande est déjà en cours.");return;}
        try{WORKER.execute(()->{try{JsonObject result=job.call();p.getServer().execute(()->{BUSY.remove(p.getUUID());if(!p.hasDisconnected())done.accept(result);});}
            catch(Exception failure){Npcshopkeeper.LOGGER.warn("NPC service failed: {}",failure.toString());p.getServer().execute(()->{BUSY.remove(p.getUUID());error(p,"Service indisponible ou données refusées. "+safe(failure));});}});}
        catch(RejectedExecutionException full){BUSY.remove(p.getUUID());error(p,"Service occupé. Réessaie dans un instant.");}
    }
    private static String safe(Exception e){String m=e.getMessage();if(m==null)return "Vérifie la liaison et les droits administrateur.";return m.length()>180?"Vérifie le backend et les droits du personnage.":m;}
    public static boolean admin(ServerPlayer p){return p.hasPermissions(2);}
    public static void openRoutes(ServerPlayer p){if(admin(p))metadata(p,routes(),"ROUTES");}
    public static void create(ServerPlayer p,BlockPos position){
        if(!admin(p)){error(p,"Administrateur requis.");return;}
        if(BUSY.contains(p.getUUID())){error(p,"Le placement précédent est encore en cours.");return;}
        PLACEMENT.put(p.getUUID(),new Placement(p.serverLevel().dimension().location().toString(),position));
        JsonObject data=new JsonObject();data.addProperty("id",UUID.randomUUID().toString());data.addProperty("revision",0);data.addProperty("name","Nouvelle boutique");data.addProperty("skin","textures/entity/banker.png");data.addProperty("active",true);data.add("offers",new JsonArray());
        metadata(p,data,"SHOP_EDIT");
    }
    private static void metadata(ServerPlayer p,JsonObject shop,String action){async(p,()->{JsonObject meta=EconomyBridge.request(p.getStringUUID(),"npc/catalogue",null);shop.add("cities",meta.get("cities"));return shop;},r->{inventory(p,r);reply(p,action,r);});}
    public static void open(ServerPlayer p,TradeNpcEntity npc,boolean edit){
        String id=npc.getPersistentData().getString("RenbloodShopId");if(id.isBlank())return;
        if(edit&&!admin(p)){error(p,"Administrateur requis.");return;}
        async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/shops/"+id,null),data->{if(!nearby(p,id)){error(p,"Approche-toi du marchand.");return;}configure(npc,data);data.addProperty("entity",npc.getStringUUID());inventory(p,data);reply(p,edit?"SHOP_EDIT":"SHOP_VIEW",data);});
    }
    private static void inventory(ServerPlayer p,JsonObject data){JsonArray rows=new JsonArray();for(var stack:p.getInventory().items){if(stack.isEmpty())continue;JsonObject item=new JsonObject();var copy=stack.copy();copy.setCount(1);item.addProperty("nbt",copy.save(new CompoundTag()).toString());item.addProperty("item_id",net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString());item.addProperty("name",stack.getHoverName().getString());rows.add(item);}data.add("inventory",rows);JsonArray skins=new JsonArray();for(var value:fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getAllNpcData().values()){Object texture=value.get("Texture");if(texture instanceof String s&&!s.isBlank())skins.add(s);}data.add("skins",skins);}
    public static boolean nearby(ServerPlayer p,String id){return NpcEntityIndex.all().stream().anyMatch(n->n.level()==p.level()&&!n.isRemoved()&&id.equals(n.getPersistentData().getString("RenbloodShopId"))&&n.distanceToSqr(p)<100);}
    public static void handle(ServerPlayer p,String action,JsonObject data)throws Exception{
        switch(action){
            case "SHOP_CREATE" -> {HitResult hit=p.pick(6,0,false);create(p,hit instanceof BlockHitResult b?b.getBlockPos().above():p.blockPosition());}
            case "SHOP_REFRESH" -> {String id=data.get("id").getAsString();if(!nearby(p,id)){error(p,"Approche-toi du marchand pour actualiser son catalogue.");return;}async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/shops/"+id,null),r->{inventory(p,r);reply(p,admin(p)&&data.has("edit")&&data.get("edit").getAsBoolean()?"SHOP_EDIT":"SHOP_VIEW",r);});}
            case "SHOP_SAVE" -> {
                if(!admin(p))return;
                String id=UUID.fromString(data.get("id").getAsString()).toString();
                boolean exists=nearby(p,id);Placement spot=PLACEMENT.get(p.getUUID());
                if(!exists&&(spot==null||!spot.world.equals(p.serverLevel().dimension().location().toString())||p.distanceToSqr(spot.pos.getX(),spot.pos.getY(),spot.pos.getZ())>400)){error(p,"Recommence le placement près du point choisi.");return;}
                data.remove("cities");data.remove("inventory");data.remove("entity");data.remove("skins");
                data.addProperty("world",p.serverLevel().dimension().location().toString());
                async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/shops",data),r->{
                    TradeNpcEntity npc=NpcEntityIndex.all().stream().filter(n->id.equals(n.getPersistentData().getString("RenbloodShopId"))&&n.level()==p.level()).findFirst().orElse(null);
                    if(npc==null){if(spot==null)return;npc=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),p.level());npc.setPos(spot.pos.getX()+.5,spot.pos.getY(),spot.pos.getZ()+.5);npc.getPersistentData().putString("RenbloodShopId",id);configure(npc,r);p.serverLevel().addFreshEntity(npc);}else configure(npc,r);
                    PLACEMENT.remove(p.getUUID());inventory(p,r);reply(p,"SHOP_EDIT",r);p.sendSystemMessage(Component.literal("Boutique enregistrée.").withStyle(net.minecraft.ChatFormatting.GREEN));
                });
            }
            case "SHOP_BUY" -> {
                String id=UUID.fromString(data.get("id").getAsString()).toString();if(!nearby(p,id)){error(p,"Approche-toi du marchand pour échanger.");return;}
                EconomyBridge.shop(p,id,data,()->nearby(p,id)).whenComplete((r,e)->p.getServer().execute(()->{
                    if(e!=null){error(p,"Échange non confirmé. Consulte le reçu ou recharge la boutique.");return;}
                    async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/shops/"+id,null),v->reply(p,"SHOP_VIEW",v));
                }));
            }
            case "ROUTES" -> openRoutes(p);
            case "ROUTE_SAVE" -> {if(!admin(p))return;String id=data.get("id").getAsString(),city=data.has("city_id")?data.get("city_id").getAsString():"";
                async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/catalogue"+(city.isBlank()?"":"?city_id="+java.net.URLEncoder.encode(city,java.nio.charset.StandardCharsets.UTF_8)),null),r->{
                    for(var road:Npcshopkeeper.COMMERCIAL_ROADS)if(road.getId().equals(id))road.setCityId(city);
                    var repo=new fr.renblood.npcshopkeeper.data.io.JsonRepository<fr.renblood.npcshopkeeper.data.commercial.CommercialRoad>(java.nio.file.Paths.get(fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.PATH_COMMERCIAL),"roads",j->fr.renblood.npcshopkeeper.data.commercial.CommercialRoad.fromJson(j,p.serverLevel()),fr.renblood.npcshopkeeper.data.commercial.CommercialRoad::toJson);repo.saveAll(Npcshopkeeper.COMMERCIAL_ROADS);
                    JsonObject body=routes();body.add("cities",r.get("cities"));reply(p,"ROUTES",body);
                });
            }
            case "CONTRACT_PICK", "CONTRACT_CITY" -> ContractService.handle(p,action,data);
            case "SHOP_DUPLICATE" -> {if(!admin(p))return;data.addProperty("id",UUID.randomUUID().toString());data.addProperty("revision",0);PLACEMENT.put(p.getUUID(),new Placement(p.serverLevel().dimension().location().toString(),p.blockPosition()));inventory(p,data);reply(p,"SHOP_EDIT",data);}
            default -> VoyageService.handle(p,action,data);
        }
    }
    public static JsonObject routes(){JsonObject data=new JsonObject();JsonArray rows=new JsonArray();for(var road:Npcshopkeeper.COMMERCIAL_ROADS){JsonObject r=new JsonObject();r.addProperty("id",road.getId());r.addProperty("name",road.getName());r.addProperty("city_id",road.getCityId());rows.add(r);}data.add("routes",rows);return data;}
    public static void configure(TradeNpcEntity npc,JsonObject shop){
        Map<String,Object> model=new HashMap<>();model.put("Texture",shop.get("skin").getAsString());model.put("Texts",List.of());model.put("IsShopkeeper",true);
        TradeNpc definition=new TradeNpc(shop.get("name").getAsString(),model,"boutique",npc.blockPosition());definition.setNpcId(npc.getStringUUID());npc.setTradeNpc(definition);
        npc.getPersistentData().putString("RenbloodShopName",shop.get("name").getAsString());npc.getPersistentData().putString("RenbloodShopSkin",shop.get("skin").getAsString());npc.setNoAi(true);npc.setInvulnerable(true);npc.setPersistenceRequired();
    }
    public static void restore(TradeNpcEntity npc){JsonObject d=new JsonObject();d.addProperty("name",npc.getPersistentData().getString("RenbloodShopName"));d.addProperty("skin",npc.getPersistentData().getString("RenbloodShopSkin"));configure(npc,d);}
    private record Placement(String world,BlockPos pos){}
}
