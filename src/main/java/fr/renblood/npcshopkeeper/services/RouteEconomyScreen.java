package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphics;
public final class RouteEconomyScreen extends ServiceScreen {
    private final JsonObject data;private int index,city=-1;
    public RouteEconomyScreen(JsonObject data){super("Routes · rattachement économique");this.data=data;}
    @Override protected void init(){clearWidgets();frame();var routes=data.getAsJsonArray("routes");if(routes.isEmpty()){message="Crée d’abord une route avec la baguette.";return;}var route=routes.get(index).getAsJsonObject();var cities=data.getAsJsonArray("cities");city=-1;for(int i=0;i<cities.size();i++)if(str(cities.get(i).getAsJsonObject(),"id","").equals(str(route,"city_id","")))city=i;
        button(str(route,"name","Route"),x+15,y+50,w-30,()->{index=(index+1)%routes.size();init();});
        button(city<0?"Aucune ville · cours commun":str(cities.get(city).getAsJsonObject(),"name",""),x+15,y+85,w-30,()->{city++;if(city>=cities.size())city=-1;route.addProperty("city_id",city<0?"":str(cities.get(city).getAsJsonObject(),"id",""));init();});
        button("Enregistrer le rattachement",x+15,y+125,w-30,()->request("ROUTE_SAVE",route.deepCopy()));
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){backdrop(g);label(g,"Les prochains contrats utilisent les prix de cette ville.",x+15,y+161,w-30,MUTED);label(g,"Les contrats déjà ouverts conservent leurs prix.",x+15,y+177,w-30,MUTED);super.render(g,mx,my,partial);}
}
