package fr.renblood.npcshopkeeper.manager.integration;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;

/** Optional dependency: a missing/newer Coins API fails before changing game state. */
public final class CommerceAdapter {
    private CommerceAdapter() {}
    public static void status(ServerPlayer player,String business){
        try{Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI").getMethod("refreshCommerceStatus",ServerPlayer.class,String.class).invoke(null,player,business);}catch(Exception ignored){}
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> settle(ServerPlayer player,String key,String business,String owner,
            String label,String kind,String offer,long price,int quantity,boolean sale,BooleanSupplier delivery,Runnable persist){
        try{
            Class<?> api=Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI");
            return (CompletableFuture<JsonObject>)api.getMethod("settleCommerce",ServerPlayer.class,String.class,String.class,
                String.class,String.class,String.class,String.class,long.class,int.class,boolean.class,BooleanSupplier.class,Runnable.class)
                .invoke(null,player,key,business,owner,label,kind,offer,price,quantity,sale,delivery,persist);
        }catch(Exception failure){return CompletableFuture.failedFuture(failure);}
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> cashContract(ServerPlayer player, String key,
            String offer, long amount, net.minecraft.world.item.ItemStack bonus,
            BooleanSupplier valid, Runnable complete) {
        try {
            Class<?> api = Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI");
            return (CompletableFuture<JsonObject>) api.getMethod("settleNpcCashContract", ServerPlayer.class,
                    String.class, String.class, long.class, net.minecraft.world.item.ItemStack.class,
                    BooleanSupplier.class, Runnable.class).invoke(null, player, key, offer, amount, bonus, valid, complete);
        } catch (ReflectiveOperationException failure) {
            return CompletableFuture.failedFuture(failure);
        }
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> claim(ServerPlayer player,String key,String business){
        try{return (CompletableFuture<JsonObject>)Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI")
            .getMethod("claimCommerce",ServerPlayer.class,String.class,String.class).invoke(null,player,key,business);}
        catch(Exception failure){return CompletableFuture.failedFuture(failure);}
    }
    public static long amount(double value){
        if(!Double.isFinite(value)||value<1||value>9007199254740991D||value!=Math.rint(value))throw new IllegalArgumentException("Montant entier positif en fer requis");
        return (long)value;
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> refund(ServerPlayer player,String key,String business,String original,BooleanSupplier delivery,Runnable persist){
        try{return (CompletableFuture<JsonObject>)Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI")
            .getMethod("refundCommerce",ServerPlayer.class,String.class,String.class,String.class,BooleanSupplier.class,Runnable.class)
            .invoke(null,player,key,business,original,delivery,persist);}
        catch(Exception failure){return CompletableFuture.failedFuture(failure);}
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> customer(ServerPlayer player,String key,String business,String owner,String label,long price,BooleanSupplier delivery,Runnable persist){
        try{return (CompletableFuture<JsonObject>)Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI")
            .getMethod("settleInnCustomer",ServerPlayer.class,String.class,String.class,String.class,String.class,long.class,BooleanSupplier.class,Runnable.class)
            .invoke(null,player,key,business,owner,label,price,delivery,persist);}
        catch(Exception failure){return CompletableFuture.failedFuture(failure);}
    }
    @SuppressWarnings("unchecked")
    public static CompletableFuture<JsonObject> passive(net.minecraft.server.MinecraftServer server,String owner,String key,String business,String label,long price){
        try{return (CompletableFuture<JsonObject>)Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI")
            .getMethod("settlePassiveInn",net.minecraft.server.MinecraftServer.class,String.class,String.class,String.class,String.class,long.class)
            .invoke(null,server,owner,key,business,label,price);}
        catch(Exception failure){return CompletableFuture.failedFuture(failure);}
    }
}
