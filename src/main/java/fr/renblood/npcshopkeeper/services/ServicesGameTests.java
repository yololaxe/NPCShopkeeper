package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("npcshopkeeper")
@PrefixGameTestTemplate(false)
public final class ServicesGameTests {
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void lotButtonsRespectStockAndTransactionLimit(GameTestHelper h){
        h.assertTrue(ShopQuantityRules.limit(64,null)==36,"Large lots stay within the backend 2304-item limit");
        h.assertTrue(ShopQuantityRules.limit(4,10)==2&&ShopQuantityRules.limit(4,0)==0,"Only complete stocked lots can be purchased");
        h.assertTrue(ShopQuantityRules.adjust("1",-1,64)==1&&ShopQuantityRules.adjust("64",1,64)==64,"Buttons clamp at both bounds");
        h.assertTrue(ShopQuantityRules.adjust("",1,64)==2,"A temporarily empty input recovers predictably");h.succeed();
    }

    @GameTest(template="services_empty",timeoutTicks=30)
    public static void catalogueCacheIsShortLivedAndPrivate(GameTestHelper h){
        var clock=new java.util.concurrent.atomic.AtomicLong();var cache=new ShopCatalogueCache(clock::get,100,2);var a=java.util.UUID.randomUUID();var b=java.util.UUID.randomUUID();var value=new JsonObject();value.addProperty("name","original");
        cache.put(a,"shop",value,cache.generation());h.assertTrue(cache.get(b,"shop")==null,"Snapshots never cross players");
        cache.get(a,"shop").addProperty("name","changed");h.assertTrue(cache.get(a,"shop").get("name").getAsString().equals("original"),"UI decorations do not corrupt cached data");
        clock.set(100);h.assertTrue(cache.get(a,"shop")==null,"Snapshot expires exactly at TTL boundary");
        long pending=cache.generation();cache.invalidate("shop");cache.put(a,"shop",value,pending);h.assertTrue(cache.get(a,"shop")==null,"An in-flight quote cannot repopulate the cache after a purchase");
        cache.put(a,"first",value,cache.generation());cache.put(a,"second",value,cache.generation());cache.get(a,"first");cache.put(a,"third",value,cache.generation());h.assertTrue(cache.get(a,"second")==null,"Cache stays bounded and retains recently read entries");cache.clear();h.assertTrue(cache.get(a,"first")==null,"World stop clears cached data");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void stationaryGazeTurnsWithoutWalking(GameTestHelper h)throws Exception{
        var npc=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),h.getLevel());npc.setNoAi(true);npc.setPos(0,64,0);npc.setYRot(0);
        var player=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"gaze-fixture"));player.setPos(3,64,0);
        var turn=TradeNpcEntity.class.getDeclaredMethod("turnTowards",net.minecraft.world.entity.player.Player.class);turn.setAccessible(true);turn.invoke(npc,player);
        h.assertTrue(npc.getYRot()<0&&npc.getYRot()>=-8,"Gaze turns smoothly toward the player");h.assertTrue(npc.isNoAi()&&npc.getX()==0&&npc.getZ()==0,"Facing a customer does not enable wandering");h.succeed();
    }

    @GameTest(template="services_empty",timeoutTicks=30)
    public static void removedNpcIdentitySurvivesRestart(GameTestHelper h){
        var id=java.util.UUID.randomUUID();var ids=new net.minecraft.nbt.ListTag();ids.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));var tag=new CompoundTag();tag.put("ids",ids);
        var deleted=fr.renblood.npcshopkeeper.command.RoadNpcAdmin.Deleted.load(tag);
        var restored=fr.renblood.npcshopkeeper.command.RoadNpcAdmin.Deleted.load(deleted.save(new CompoundTag()));
        h.assertTrue(restored.contains(id),"Deleted UUID is suppressed after world reload");h.assertTrue(!restored.contains(java.util.UUID.randomUUID()),"Other NPC identities remain allowed");var root=h.getLevel().getServer().getCommands().getDispatcher().getRoot().getChild("ns");h.assertTrue(root.getChild("npc").getChild("info")!=null&&root.getChild("npc").getChild("remove")!=null,"New admin commands registered under /ns");h.succeed();
    }

    @GameTest(template="services_empty",timeoutTicks=30)
    public static void countdownUsesRealClock(GameTestHelper h){
        h.assertTrue(VoyageRules.secondsUntil(12000,10800,3)==180,"Slowed day gives three real minutes, not one");
        h.assertTrue(VoyageRules.secondsUntil(36000,35600,3)==60,"Reminder starts in the last real minute on any day");
        h.assertTrue(VoyageRules.secondsUntil(12000,12001,3)==0,"Expired departure never becomes tomorrow");
        h.assertTrue(VoyageRules.secondsUntil(12000,11999,.1)==1,"Subsecond remainder rounds up");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void unpricedArticlesStayManual(GameTestHelper h){
        JsonObject data=new JsonObject(),row=new JsonObject();row.addProperty("item_id","minecraft:candle");
        h.assertTrue(!ShopPriceRules.automatic(data,row),"Unknown item cannot use automatic prices");
        JsonArray refs=new JsonArray();JsonObject ref=new JsonObject();ref.addProperty("item_id","minecraft:candle");ref.add("price",JsonNull.INSTANCE);refs.add(ref);data.add("references",refs);
        h.assertTrue(!ShopPriceRules.automatic(data,row),"City without quote stays manual");
        ref.addProperty("price","67");h.assertTrue(ShopPriceRules.automatic(data,row),"Known local quote enables automatic pricing");
        h.assertTrue(row.get("buy_price").getAsString().equals("67")&&row.get("sell_price").getAsString().equals("53"),"Manual fields receive current automatic amounts");
        ref.add("price",JsonNull.INSTANCE);ShopPriceRules.automatic(data,row);h.assertTrue(row.get("buy_price").getAsLong()==0,"Lost automatic quote does not become a stale manual price");
        row.addProperty("buy_price","42");ShopPriceRules.automatic(data,row);h.assertTrue(row.get("buy_price").getAsLong()==42,"Bulk mode preserves explicit manual prices without a quote");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void passageClock(GameTestHelper h){
        h.assertTrue(VoyageRules.open(2000,2000,11500),"Opening boundary");
        h.assertTrue(!VoyageRules.open(11500,2000,11500),"Closed at boarding boundary");
        h.assertTrue(VoyageRules.next(11000,11500,12000)==12000,"Same-day departure");
        h.assertTrue(VoyageRules.next(11500,11500,12000)==36000,"Next-day departure");
        h.assertTrue(VoyageRules.next(35000,11500,12000)==36000,"Absolute world day preserved");
        h.assertTrue(VoyageRules.price(101,50,0)==3,"Fare rounds up");
        h.assertTrue(VoyageRules.price(101,50,20)==20,"Manual fare overrides distance");
        h.assertTrue(VoyageRules.hour(18,0)==12000,"Dusk at 18:00");h.assertTrue(VoyageRules.clock(VoyageRules.hour(17,31)).equals("17:31"),"Minute precision roundtrips in schedule editor");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void legacyAndConfiguredPorts(GameTestHelper h){
        Port old=Port.fromJson(JsonParser.parseString("{\"name\":\"Triomphe\",\"x\":0,\"y\":64,\"z\":0,\"dimension\":\"minecraft:overworld\"}").getAsJsonObject());
        h.assertTrue(old.getOpens()==2000&&old.getDeparture()==12000,"Legacy port receives safe default schedule");
        h.assertTrue(old.getCounty().equals("Mofrage"),"County comes from existing atlas");
        JsonObject json=old.toJson();json.addProperty("company","Compagnie de recette");json.addProperty("cityId","fixture-city");json.addProperty("departure",13000);Port port=Port.fromJson(json);port.validate();
        h.assertTrue(Port.fromJson(port.toJson()).getDeparture()==13000,"Edited schedule roundtrips");h.assertTrue(port.getCityId().equals("fixture-city"),"City binding roundtrips");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void merchantIdentitySurvivesEntitySave(GameTestHelper h){
        TradeNpcEntity npc=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),h.getLevel());npc.getPersistentData().putString("RenbloodShopId","fixture");JsonObject shop=new JsonObject();shop.addProperty("name","Marchand de recette");shop.addProperty("skin","textures/entity/banker.png");WorkshopServer.configure(npc,shop);
        CompoundTag tag=npc.saveWithoutId(new CompoundTag());TradeNpcEntity restored=new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(),h.getLevel());restored.load(tag);
        h.assertTrue(restored.getPersistentData().getString("RenbloodShopId").equals("fixture"),"Shop identity survives entity save");h.assertTrue(restored.getTexture().equals("textures/entity/banker.png"),"Skin restored without API query");h.assertTrue(restored.isNoAi(),"Permanent merchant stays at its counter");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void reservationsSurviveSavedData(GameTestHelper h){
        VoyageData data=new VoyageData();JsonObject ticket=new JsonObject();ticket.addProperty("id","fixture-ticket");ticket.addProperty("state","MISSED");ticket.addProperty("price","12345678901234567");ticket.addProperty("departure",36000);data.tickets.add("player",ticket);
        VoyageData restored=VoyageData.load(data.save(new CompoundTag()));var got=restored.tickets.getAsJsonObject("player");h.assertTrue(got.get("state").getAsString().equals("MISSED"),"Missed ticket remains reportable after reload");h.assertTrue(got.get("price").getAsString().equals("12345678901234567"),"Fare is preserved exactly");h.succeed();
    }
    @GameTest(template="services_empty",timeoutTicks=30)
    public static void playerControlledProfilesCannotJoinTradeRoutes(GameTestHelper h){
        var profile=new fr.renblood.npcshopkeeper.data.api.NpcTemplate();
        profile.npc_id="fixture-controlled";profile.name="Recette profil joueur";profile.type="SHOPKEEPER";profile.enabled=true;
        fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.addNpcFromTemplate(profile);
        h.assertTrue(fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getInactiveNpcs().contains(profile.name),"Enabled merchant is eligible");
        profile.enabled=false;
        fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.addNpcFromTemplate(profile);
        h.assertTrue(!fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getInactiveNpcs().contains(profile.name),"Controlled/archived merchant is not eligible");
        fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.deactivateNpc(profile.name);
        h.assertTrue(!fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getInactiveNpcs().contains(profile.name),"Deactivation cannot reintroduce a disabled profile");h.succeed();
    }
}
