package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class ContractService {
    private static final Map<String,Map<String,Long>> PRICES=new HashMap<>();
    public static String city(TradeNpcEntity npc){return npc.getPersistentData().getString("RenbloodCityId");}
    public static boolean open(ServerPlayer p,TradeNpcEntity npc){
        if(npc.getPersistentData().getBoolean("IsDecor"))return false;
        if(RuntimeDataCache.getOpenHistory(npc.getNpcId()).isPresent())return false;
        load(p,npc,null);return true;
    }
    public static void load(ServerPlayer p,TradeNpcEntity npc,String chosen){
        String city=city(npc);
        WorkshopServer.async(p,()->EconomyBridge.request(p.getStringUUID(),"npc/catalogue"+(city.isBlank()?"":"?city_id="+java.net.URLEncoder.encode(city,java.nio.charset.StandardCharsets.UTF_8)),null),metadata->{
            if(npc.isRemoved()||npc.level()!=p.level()||npc.distanceToSqr(p)>100)return;
            Map<String,Long> prices=new HashMap<>();for(var r:metadata.getAsJsonArray("references")){var ref=r.getAsJsonObject();if(ref.has("price")&&!ref.get("price").isJsonNull())prices.put(ref.get("item_id").getAsString(),ref.get("price").getAsLong());}
            if(PRICES.size()>512)PRICES.clear();PRICES.put(npc.getNpcId(),prices);
            var trades=RuntimeDataCache.getTradesByCategory(npc.getTradeCategory());
            if(npc.getTradeNpc()!=null&&npc.getTradeNpc().isRouteNpc()&&trades.size()>1){var chosenTrade=npc.getTradeNpc().getTrade();var pick=chosenTrade==null?trades.get(0):chosenTrade;trades=java.util.List.of(pick);}
            if(chosen!=null){if(trades.stream().noneMatch(t->t.getName().equals(chosen)))return;fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure.execute(p.level(),p.getX(),p.getY(),p.getZ(),chosen,p,npc.getNpcId(),npc.getNpcName());return;}
            JsonObject d=new JsonObject();d.addProperty("entity",npc.getStringUUID());d.addProperty("name",npc.getNpcName());d.addProperty("city_id",city);d.add("cities",metadata.get("cities"));d.addProperty("admin",WorkshopServer.admin(p));
            JsonArray rows=new JsonArray();for(var t:trades){JsonObject r=new JsonObject();r.addProperty("name",t.getName());r.addProperty("items",t.getTrades().stream().map(i->i.getItem()+" × "+i.getMin()+"–"+i.getMax()).reduce((a,b)->a+", "+b).orElse(""));rows.add(r);}d.add("contracts",rows);WorkshopServer.reply(p,"CONTRACTS",d);
        });
    }
    public static org.apache.commons.lang3.tuple.Pair<Long,Long> price(String npc,String item,ServerPlayer player){
        Map<String,Long> prices=PRICES.get(npc);if(prices!=null){long value=prices.getOrDefault(item,0L);return org.apache.commons.lang3.tuple.Pair.of(value,value);}
        return fr.renblood.npcshopkeeper.manager.trade.PriceReferenceManager.findReferenceByItem(item,player);
    }
    public static void handle(ServerPlayer p,String action,JsonObject body){
        TradeNpcEntity npc=NpcEntityIndex.all().stream().filter(n->n.getStringUUID().equals(body.get("entity").getAsString())&&n.level()==p.level()&&n.distanceToSqr(p)<100).findFirst().orElse(null);if(npc==null){WorkshopServer.error(p,"Approche-toi du marchand pour choisir une commande.");return;}
        if(action.equals("CONTRACT_PICK"))load(p,npc,body.get("name").getAsString());
        else if(action.equals("CONTRACT_CITY")&&WorkshopServer.admin(p)){String city=body.get("city_id").getAsString();npc.getPersistentData().putString("RenbloodCityId",city);load(p,npc,null);}
    }
    public static JsonObject terms(String npc,String offer,fr.renblood.npcshopkeeper.data.trade.TradeHistory history){
        JsonObject d=new JsonObject();d.addProperty("offer",offer);d.addProperty("city_id",history.getCityId());
        JsonArray rows=new JsonArray();for(var i:history.getTradeItems()){JsonObject row=new JsonObject();row.addProperty("item_id",i.getItem());row.addProperty("quantity",i.getQuantity());row.addProperty("price",Long.toString(i.getPrice()));rows.add(row);}d.add("items",rows);return d;
    }
}
