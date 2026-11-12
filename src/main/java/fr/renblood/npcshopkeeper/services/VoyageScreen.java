package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
public final class VoyageScreen extends ServiceScreen {
    final boolean editor;private JsonObject data,selected;private String county="";private double cx=577,cy=804,scale,targetX=577,targetY=804,targetScale;private int mapX,mapY,mapW,mapH,panel,pw;private EditBox city,company,opens,closes,departure,fare;private int destinationPage;
    private static final ResourceLocation MAP=new ResourceLocation("npcshopkeeper","textures/gui/realm_atlas.png");
    public VoyageScreen(JsonObject data,boolean editor){super(editor?"Ports et traversées":"Les traversées du royaume");this.data=data;this.editor=editor;}
    public void accept(JsonObject d){data=d;busy=false;message="";init();}
    private JsonObject origin(){return data.getAsJsonObject("origin");}
    private JsonArray destinations(){return data.getAsJsonArray("destinations");}
    private void send(String action){JsonObject q=new JsonObject();q.addProperty("origin",str(origin(),"name",""));request(action,q);}
    @Override protected void init(){clearWidgets();frame();mapX=x+10;mapY=y+61;mapW=(int)(w*.60)-15;mapH=h-100;panel=x+mapW+23;pw=x+w-panel-12;
        if(targetScale==0){scale=targetScale=Math.min(mapW/1154.0,mapH/1608.0)*.95;}
        button("Royaume",mapX,y+34,Math.min(90,mapW/2),()->{county="";selected=null;targetX=577;targetY=804;targetScale=Math.min(mapW/1154.0,mapH/1608.0)*.95;init();});
        java.util.Set<String> counties=new java.util.LinkedHashSet<>();for(var c:RealmAtlas.cities())counties.add(str(c.getAsJsonObject(),"county",""));int n=0,bw=(mapW-96)/4;for(String name:counties){final String countyName=name;button(name,mapX+96+n++*bw,y+34,bw-3,()->focusCounty(countyName));}
        button("Actualiser",panel,y+34,pw,()->send("VOYAGE_STATUS"));
        if(editor){
            city=field("Ville",str(origin(),"cityName",str(origin(),"name","")),panel,mapY,pw,64);company=field("Compagnie",str(origin(),"company",""),panel,mapY+30,pw,100);
            opens=field("Ouverture",hour(origin().get("opens").getAsInt()),panel,mapY+65,pw/3-3,5);closes=field("Clôture",hour(origin().get("closes").getAsInt()),panel+pw/3,mapY+65,pw/3-3,5);departure=field("Départ",hour(origin().get("departure").getAsInt()),panel+pw*2/3,mapY+65,pw/3,5);
            button(flag(origin(),"enabled",true)?"Service ouvert":"Service suspendu",panel,mapY+92,pw,()->{storeFields();origin().addProperty("enabled",!flag(origin(),"enabled",true));init();});
            if(selected!=null){fare=field("Tarif fer · 0 auto",str(selected,"fixedFare","0"),panel,mapY+139,pw,10);
                button(flag(selected,"served",true)?"Liaison : ouverte":"Liaison : fermée",panel,mapY+164,pw,()->{storeFields();toggleDestination();init();});}
            button("Enregistrer le port",panel,y+h-74,pw,()->{try{storeFields();JsonObject q=new JsonObject();q.addProperty("origin",str(origin(),"name",""));q.add("port",origin().deepCopy());request("HARBOR_SAVE",q);}catch(Exception invalid){error("Heures au format HH:MM ; ouverture < clôture ≤ départ.");}});
            button("Simuler mon départ",panel,y+h-48,pw,()->send("VOYAGE_TEST"));
        }else if(data.has("ticket")&&!Set.of("ARRIVED","REFUNDED").contains(str(data.getAsJsonObject("ticket"),"state",""))){
            String state=str(data.getAsJsonObject("ticket"),"state","");
            if(state.equals("MISSED"))button("Reporter gratuitement",panel,mapY+85,pw,()->send("VOYAGE_REBOOK"));
            button("Annuler / rembourser",panel,mapY+112,pw,()->send("VOYAGE_CANCEL"));
        }else if(selected!=null){button("Réserver et payer",panel,mapY+107,pw,()->{JsonObject q=new JsonObject();q.addProperty("origin",str(origin(),"name",""));q.addProperty("destination",str(selected,"name",""));q.addProperty("price",str(selected,"price","0"));request("VOYAGE_BOOK",q);}).active=flag(selected,"served",false)&&flag(data,"open",false);}
        int fy=mapY+Math.min(200,mapH/2+30),rows=Math.max(1,(y+h-85-fy)/23),index=0;List<JsonObject> list=new ArrayList<>();for(var value:destinations()){var d=value.getAsJsonObject();if(county.isBlank()||str(d,"county","").equals(county))list.add(d);}
        for(int i=destinationPage*rows;i<Math.min(list.size(),(destinationPage+1)*rows);i++){JsonObject d=list.get(i);button(font.plainSubstrByWidth(str(d,"cityName",str(d,"name","")),pw-10),panel,fy+index++*23,pw,()->select(d));}
        if(list.size()>rows)button("Destinations suivantes",mapX,y+h-31,mapW,()->{destinationPage=(destinationPage+1)%((list.size()+rows-1)/rows);init();});
    }
    private void focusCounty(String name){if(editor)storeFields();county=name;selected=null;destinationPage=0;double minx=1154,maxx=0,miny=1608,maxy=0;for(var v:RealmAtlas.cities()){var c=v.getAsJsonObject();if(!str(c,"county","").equals(county))continue;double px=c.get("x").getAsDouble(),py=c.get("y").getAsDouble();minx=Math.min(minx,px);maxx=Math.max(maxx,px);miny=Math.min(miny,py);maxy=Math.max(maxy,py);}targetX=(minx+maxx)/2;targetY=(miny+maxy)/2;targetScale=Math.min(mapW/Math.max(200,maxx-minx+120),mapH/Math.max(200,maxy-miny+120));init();}
    private void select(JsonObject d){if(editor)storeFields();selected=d;county=str(d,"county","");double px=d.get("mapX").getAsDouble(),py=d.get("mapY").getAsDouble();if(px>=0&&py>=0){targetX=px;targetY=py;targetScale=Math.min(mapW/450.0,mapH/550.0);}init();}
    private void toggleDestination(){JsonArray allowed=origin().has("destinations")?origin().getAsJsonArray("destinations"):new JsonArray();if(!origin().has("destinations"))for(var d:destinations())if(flag(d.getAsJsonObject(),"served",false))allowed.add(str(d.getAsJsonObject(),"name",""));String name=str(selected,"name","");JsonArray next=new JsonArray();boolean had=false;for(var n:allowed)if(n.getAsString().equals(name))had=true;else next.add(n);if(!had)next.add(name);origin().add("destinations",next);selected.addProperty("served",!had);}
    private void storeFields(){if(!editor||city==null)return;origin().addProperty("cityName",city.getValue());origin().addProperty("company",company.getValue());origin().addProperty("opens",parseHour(opens.getValue()));origin().addProperty("closes",parseHour(closes.getValue()));origin().addProperty("departure",parseHour(departure.getValue()));
        for(var v:RealmAtlas.cities()){var c=v.getAsJsonObject();if(str(c,"name","").equalsIgnoreCase(city.getValue())){origin().addProperty("county",str(c,"county",""));origin().add("mapX",c.get("x"));origin().add("mapY",c.get("y"));break;}}
        if(selected!=null&&fare!=null){if(!origin().has("fares"))origin().add("fares",new JsonObject());long value=number(fare.getValue(),0,1000000000);origin().getAsJsonObject("fares").addProperty(str(selected,"name",""),value);selected.addProperty("fixedFare",value);}}
    private static int parseHour(String value){String[] p=value.split(":");int h=Integer.parseInt(p[0]),m=p.length>1?Integer.parseInt(p[1]):0;if(h<0||h>23||m<0||m>59)throw new IllegalArgumentException();return VoyageRules.hour(h,m);}
    private static String hour(int tick){return VoyageRules.clock(tick);}
    private double sx(double px){return mapX+mapW/2.0+(px-cx)*scale;}private double sy(double py){return mapY+mapH/2.0+(py-cy)*scale;}
    @Override protected boolean canvasClick(double mx,double my,int button){if(button==0&&mx>=mapX&&mx<mapX+mapW&&my>=mapY&&my<mapY+mapH){
        if(!county.isBlank())for(var v:destinations()){var d=v.getAsJsonObject();if(d.get("mapX").getAsDouble()<0)continue;if(Math.hypot(mx-sx(d.get("mapX").getAsDouble()),my-sy(d.get("mapY").getAsDouble()))<9){select(d);return true;}}
        double px=cx+(mx-mapX-mapW/2.0)/scale,py=cy+(my-mapY-mapH/2.0)/scale;String closest="";double dist=Double.MAX_VALUE;for(var v:RealmAtlas.cities()){var c=v.getAsJsonObject();double delta=Math.hypot(px-c.get("x").getAsDouble(),py-c.get("y").getAsDouble());if(delta<dist){dist=delta;closest=c.get("county").getAsString();}}
        county=closest;double minx=1154,maxx=0,miny=1608,maxy=0;for(var v:RealmAtlas.cities()){var c=v.getAsJsonObject();if(!str(c,"county","").equals(county))continue;double px2=c.get("x").getAsDouble(),py2=c.get("y").getAsDouble();minx=Math.min(minx,px2);maxx=Math.max(maxx,px2);miny=Math.min(miny,py2);maxy=Math.max(maxy,py2);}targetX=(minx+maxx)/2;targetY=(miny+maxy)/2;targetScale=Math.min(mapW/Math.max(200,maxx-minx+120),mapH/Math.max(200,maxy-miny+120));selected=null;destinationPage=0;init();return true;}
        return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){backdrop(g);double rate=.18;cx+=(targetX-cx)*rate;cy+=(targetY-cy)*rate;scale+=(targetScale-scale)*rate;g.enableScissor((int)(mapX*uiScale),(int)(mapY*uiScale),(int)((mapX+mapW)*uiScale),(int)((mapY+mapH)*uiScale));g.fill(mapX,mapY,mapX+mapW,mapY+mapH,0xFF142A31);g.pose().pushPose();g.pose().translate(sx(0),sy(0),0);g.pose().scale((float)scale,(float)scale,1);g.blit(MAP,0,0,0,0,1154,1608,1154,1608);g.pose().popPose();
        if(selected!=null&&origin().get("mapX").getAsDouble()>=0&&selected.get("mapX").getAsDouble()>=0){double ax=sx(origin().get("mapX").getAsDouble()),ay=sy(origin().get("mapY").getAsDouble()),bx=sx(selected.get("mapX").getAsDouble()),by=sy(selected.get("mapY").getAsDouble());for(int i=0;i<=36;i+=2){int xx=(int)(ax+(bx-ax)*i/36),yy=(int)(ay+(by-ay)*i/36);g.fill(xx,yy,xx+2,yy+2,GOLD);}}
        for(var v:destinations()){var d=v.getAsJsonObject();double px=d.get("mapX").getAsDouble(),py=d.get("mapY").getAsDouble();if(px<0||py<0)continue;int dx=(int)sx(px),dy=(int)sy(py);int color=flag(d,"served",false)?GOLD:MUTED;g.fill(dx-3,dy-3,dx+4,dy+4,color);if(!county.isBlank())label(g,str(d,"cityName",str(d,"name","")),dx+6,dy-4,90,color);}
        g.disableScissor();label(g,county.isBlank()?"Choisis un comté, puis une ville.":county,mapX+5,mapY+5,mapW-10,GOLD);
        if(editor){label(g,"Ouverture / clôture / départ",panel,mapY+54,pw,MUTED);if(selected!=null)label(g,"Tarif en fer · 0 = distance",panel,mapY+128,pw,MUTED);}else{if(data.has("ticket")&&!Set.of("ARRIVED","REFUNDED").contains(str(data.getAsJsonObject("ticket"),"state",""))){var t=data.getAsJsonObject("ticket");label(g,"Billet : "+str(t,"destination",""),panel,mapY,pw,GOLD);label(g,"État : "+stateLabel(str(t,"state","")),panel,mapY+17,pw,MUTED);}else if(selected!=null){label(g,str(selected,"cityName",str(selected,"name","")),panel,mapY,pw,GOLD);label(g,money(str(selected,"price","0")),panel,mapY+20,pw,PAPER);label(g,flag(selected,"served",false)?"Départ à "+hour(origin().get("departure").getAsInt()):"Ville non desservie",panel,mapY+40,pw,MUTED);}label(g,flag(data,"open",false)?"Billetterie ouverte":"Billetterie fermée",panel,mapY+60,pw,MUTED);}
        super.render(g,mx,my,partial);
    }
}
