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
    public static void passageClock(GameTestHelper h){
        h.assertTrue(VoyageRules.open(2000,2000,11500),"Opening boundary");
        h.assertTrue(!VoyageRules.open(11500,2000,11500),"Closed at boarding boundary");
        h.assertTrue(VoyageRules.next(11000,11500,12000)==12000,"Same-day departure");
        h.assertTrue(VoyageRules.next(11500,11500,12000)==36000,"Next-day departure");
        h.assertTrue(VoyageRules.next(35000,11500,12000)==36000,"Absolute world day preserved");
        h.assertTrue(VoyageRules.price(101,50,0)==3,"Fare rounds up");
        h.assertTrue(VoyageRules.price(101,50,20)==20,"Manual fare overrides distance");
        h.assertTrue(VoyageRules.hour(18,0)==12000,"Dusk at 18:00");h.succeed();
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
