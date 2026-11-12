package fr.renblood.npcshopkeeper.services;
public final class ShopQuantityRules {
    public static int limit(int lot,Integer stock){if(lot<1)return 0;int maximum=Math.min(64,2304/lot);return stock==null?maximum:Math.max(0,Math.min(maximum,stock/lot));}
    public static int adjust(String value,int delta,int maximum){int n;try{n=Integer.parseInt(value);}catch(NumberFormatException invalid){n=1;}return (int)Math.max(1,Math.min((long)maximum,(long)n+delta));}
}
