package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
public final class ServiceClient {
    public static void receive(String action,String json){
        JsonObject data=JsonParser.parseString(json).getAsJsonObject();var mc=Minecraft.getInstance();
        if(action.equals("ERROR")){if(mc.screen instanceof ServiceScreen s)s.error(data.get("error").getAsString());else if(mc.player!=null)mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(data.get("error").getAsString()).withStyle(net.minecraft.ChatFormatting.RED),false);return;}
        if(action.equals("SAILING")){mc.setScreen(new SailingScreen(data));return;}
        if(action.equals("ARRIVED")){if(mc.screen instanceof SailingScreen)mc.setScreen(null);return;}
        if(action.startsWith("SHOP_")){if(mc.screen instanceof ShopScreen s&&s.editor==action.equals("SHOP_EDIT"))s.accept(data);else mc.setScreen(new ShopScreen(data,action.equals("SHOP_EDIT")));}
        else if(action.equals("ROUTES"))mc.setScreen(new RouteEconomyScreen(data));
        else if(action.equals("CONTRACTS"))mc.setScreen(new ContractCatalogueScreen(data));
        else if(action.equals("VOYAGE")||action.equals("HARBOR_EDIT")){if(mc.screen instanceof VoyageScreen s&&s.editor==action.equals("HARBOR_EDIT"))s.accept(data);else mc.setScreen(new VoyageScreen(data,action.equals("HARBOR_EDIT")));}
    }
}
