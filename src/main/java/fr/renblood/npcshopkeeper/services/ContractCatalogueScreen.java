package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphics;
public final class ContractCatalogueScreen extends ServiceScreen {
    private final JsonObject data;private int page;
    public ContractCatalogueScreen(JsonObject data){super("Commandes — "+str(data,"name","Marchand"));this.data=data;}
    @Override protected void init(){clearWidgets();frame();var rows=data.getAsJsonArray("contracts");int count=Math.max(1,Math.min(6,(h-105)/48));
        for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var r=rows.get(i).getAsJsonObject();button(str(r,"name","Commande"),x+12,y+50+(i-page*count)*48,w-24,()->{JsonObject q=new JsonObject();q.add("entity",data.get("entity"));q.add("name",r.get("name"));request("CONTRACT_PICK",q);});}
        button("Précédent",x+12,y+h-47,100,()->{page=Math.max(0,page-1);init();});button("Suivant",x+w-112,y+h-47,100,()->{page=Math.min(Math.max(0,(rows.size()-1)/count),page+1);init();});
        if(flag(data,"admin",false))button("Ville / cours commun",x+w/2-85,y+h-47,170,()->{String current=str(data,"city_id","");var cities=data.getAsJsonArray("cities");int next=0;for(int i=0;i<cities.size();i++)if(str(cities.get(i).getAsJsonObject(),"id","").equals(current))next=i+1;JsonObject q=new JsonObject();q.add("entity",data.get("entity"));q.addProperty("city_id",next<cities.size()?str(cities.get(next).getAsJsonObject(),"id",""):"");request("CONTRACT_CITY",q);});
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){backdrop(g);String name="Cours commun";for(var c:data.getAsJsonArray("cities"))if(str(c.getAsJsonObject(),"id","").equals(str(data,"city_id","")))name=str(c.getAsJsonObject(),"name","");label(g,name+" · une commande choisie à la fois",x+12,y+33,w-24,MUTED);int count=Math.max(1,Math.min(6,(h-105)/48));var rows=data.getAsJsonArray("contracts");for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++)label(g,str(rows.get(i).getAsJsonObject(),"items",""),x+15,y+74+(i-page*count)*48,w-30,MUTED);super.render(g,mx,my,partial);}
}
