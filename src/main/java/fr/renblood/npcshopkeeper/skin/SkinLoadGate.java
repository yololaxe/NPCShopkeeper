package fr.renblood.npcshopkeeper.skin;

import java.util.LinkedHashMap;
import java.util.Map;

/** One queued/running request per skin, with a bounded failure cache. */
public final class SkinLoadGate {
    private static final class State { boolean loading; int failures; long retryAt; }
    private final Map<String,State> states=new LinkedHashMap<>(32,.75f,true);

    public synchronized boolean begin(String key,long now) {
        State state=states.get(key);
        if(state!=null&&(state.loading||now<state.retryAt))return false;
        if(state==null){
            if(states.size()>=512){
                var iterator=states.entrySet().iterator();
                while(iterator.hasNext())if(!iterator.next().getValue().loading){iterator.remove();break;}
                if(states.size()>=512)return false;
            }
            state=new State();states.put(key,state);
        }
        state.loading=true;return true;
    }

    public synchronized void success(String key){states.remove(key);}

    public synchronized void failure(String key,long now,long minimumDelay) {
        State state=states.get(key);
        if(state==null)return;
        state.loading=false;state.failures=Math.min(6,state.failures+1);
        long delay=Math.min(900_000L,30_000L<<(state.failures-1));
        state.retryAt=now+Math.max(delay,minimumDelay);
    }
}
