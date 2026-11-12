package fr.renblood.npcshopkeeper.services;
/** Pure rules: Minecraft day starts at 06:00; dusk is tick 12000. */
public final class VoyageRules {
    public static final long DAY=24000;
    public static boolean open(long day,int opens,int closes){long time=Math.floorMod(day,DAY);return time>=opens&&time<closes;}
    public static long next(long day,int closes,int departure){long start=Math.floorDiv(day,DAY)*DAY;return start+departure+(Math.floorMod(day,DAY)>=closes?DAY:0);}
    public static long price(double distance,int blocks,long fixed){if(fixed>0)return fixed;if(blocks<1||!Double.isFinite(distance))throw new IllegalArgumentException();return Math.max(1,(long)Math.ceil(distance/blocks));}
    public static String clock(int tick){int minute=((int)Math.round(tick*60.0/1000)+360)%1440;return String.format("%02d:%02d",minute/60,minute%60);}
    public static int hour(int h,int m){return Math.floorMod((int)Math.round((h-6)*1000+m*1000.0/60),24000);}
}
