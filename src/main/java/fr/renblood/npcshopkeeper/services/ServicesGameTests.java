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
}
