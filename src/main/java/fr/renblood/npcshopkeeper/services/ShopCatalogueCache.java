package fr.renblood.npcshopkeeper.services;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.LongSupplier;
/** Short-lived display snapshots only. Purchases always revalidate against the backend. */
public final class ShopCatalogueCache {
    private record Key(UUID player,String shop){}
    private record Entry(JsonObject data,long expires){}
    private final LinkedHashMap<Key,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private final LongSupplier clock;private final long ttl;private final int capacity;private long generation;
    public ShopCatalogueCache(LongSupplier clock,long ttl,int capacity){this.clock=clock;this.ttl=ttl;this.capacity=capacity;}
    public long generation(){return generation;}
    public JsonObject get(UUID player,String shop){Key key=new Key(player,shop);Entry entry=entries.get(key);if(entry==null)return null;if(clock.getAsLong()>=entry.expires){entries.remove(key);return null;}return entry.data.deepCopy();}
    public void put(UUID player,String shop,JsonObject data,long expectedGeneration){if(expectedGeneration!=generation)return;entries.put(new Key(player,shop),new Entry(data.deepCopy(),clock.getAsLong()+ttl));while(entries.size()>capacity)entries.remove(entries.keySet().iterator().next());}
    public void invalidate(String shop){generation++;entries.keySet().removeIf(k->k.shop.equals(shop));}
    public void clear(){generation++;entries.clear();}
}
