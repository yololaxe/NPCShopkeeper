package fr.renblood.npcshopkeeper.services;
/** Pure rules: Minecraft day starts at 06:00; dusk is tick 12000. */
public final class VoyageRules {
    public static final long DAY=24000;
    public static boolean open(long day,int opens,int closes){long time=Math.floorMod(day,DAY);return time>=opens&&time<closes;}
    public static long next(long day,int closes,int departure){long start=Math.floorDiv(day,DAY)*DAY;return start+departure+(Math.floorMod(day,DAY)>=closes?DAY:0);}
    public static long price(double distance,int blocks,long fixed){if(fixed>0)return fixed;if(blocks<1||!Double.isFinite(distance))throw new IllegalArgumentException();return Math.max(1,(long)Math.ceil(distance/blocks));}
    public static int hour(int h,int m){return Math.floorMod((h-6)*1000+m*1000/60,24000);}
}
