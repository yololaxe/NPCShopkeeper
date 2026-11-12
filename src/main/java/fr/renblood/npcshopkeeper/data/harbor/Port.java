package fr.renblood.npcshopkeeper.data.harbor;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import java.util.*;
public class Port {
    private String name;private int x,y,z;private String dimension;
    private String cityId,cityName,county,company;
    private Integer opens,closes,departure,revision;
    public int getRevision(){return revision==null?0:revision;}public void setRevision(int value){revision=value;}private Double mapX,mapY;private Boolean enabled;
    private List<String> destinations;private Map<String,Long> fares;
    public Port(String name,BlockPos pos,String dimension){this.name=name;this.x=pos.getX();this.y=pos.getY();this.z=pos.getZ();this.dimension=dimension;}
    public String getName(){return name;}public BlockPos getPos(){return new BlockPos(x,y,z);}public String getDimension(){return dimension;}
    public String getCityName(){return cityName==null||cityName.isBlank()?name:cityName;}public String getCityId(){return cityId==null?"":cityId;}
    public String getCounty(){return county==null||county.isBlank()?fr.renblood.npcshopkeeper.services.RealmAtlas.county(getCityName()):county;}
    public String getCompany(){return company==null||company.isBlank()?"Compagnie de "+name:company;}
    public int getOpens(){return opens==null?2000:opens;}public int getCloses(){return closes==null?11500:closes;}public int getDeparture(){return departure==null?12000:departure;}
    public boolean isEnabled(){return enabled==null||enabled;}public double getMapX(){return mapX==null?fr.renblood.npcshopkeeper.services.RealmAtlas.coordinate(getCityName(),"x"):mapX;}
    public double getMapY(){return mapY==null?fr.renblood.npcshopkeeper.services.RealmAtlas.coordinate(getCityName(),"y"):mapY;}
    public boolean serves(String name){return destinations==null||destinations.contains(name);}
    public long getFixedFare(String name){return fares==null?0:fares.getOrDefault(name,0L);}
    public void validate(){if(name==null||name.isBlank()||name.length()>100||dimension==null||getOpens()<0||getCloses()<=getOpens()||getDeparture()<getCloses()||getDeparture()>=24000)throw new IllegalArgumentException("Horaires invalides (ouverture < clôture ≤ départ).");
        if(getMapX()<0||getMapX()>1154||getMapY()<0||getMapY()>1608)throw new IllegalArgumentException("Point hors de la carte.");
        if(destinations!=null&&destinations.size()>200)throw new IllegalArgumentException("Trop de destinations");
        if(fares!=null&&fares.values().stream().anyMatch(v->v==null||v<0||v>1000000000L))throw new IllegalArgumentException("Tarif invalide");
    }
    public static Port fromJson(JsonObject json){return new Gson().fromJson(json,Port.class);}
    public JsonObject toJson(){JsonObject j=(JsonObject)new Gson().toJsonTree(this);j.addProperty("cityName",getCityName());j.addProperty("county",getCounty());j.addProperty("company",getCompany());j.addProperty("opens",getOpens());j.addProperty("closes",getCloses());j.addProperty("departure",getDeparture());j.addProperty("mapX",getMapX());j.addProperty("mapY",getMapY());j.addProperty("enabled",isEnabled());j.addProperty("revision",getRevision());return j;}
}
