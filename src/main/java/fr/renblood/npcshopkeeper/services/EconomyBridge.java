package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
public final class EconomyBridge {
    private static Class<?> api()throws Exception{return Class.forName("fr.renblood.medievalcoins.bank.NpcCommerceGateway");}
    public static JsonObject request(String uuid,String path,JsonObject body)throws Exception{
        try{return (JsonObject)api().getMethod("request",String.class,String.class,JsonObject.class).invoke(null,uuid,path,body);}
        catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause() instanceof Exception e?e:failure;}
    }
    @SuppressWarnings("unchecked") public static CompletableFuture<JsonObject> shop(ServerPlayer p,String id,JsonObject body,BooleanSupplier valid)throws Exception{
        return (CompletableFuture<JsonObject>)api().getMethod("shop",ServerPlayer.class,String.class,JsonObject.class,BooleanSupplier.class).invoke(null,p,id,body,valid);
    }
    @SuppressWarnings("unchecked") public static CompletableFuture<JsonObject> ticket(ServerPlayer p,String key,String port,long price,BooleanSupplier valid,Runnable issue,Runnable persist,boolean refund,String original)throws Exception{
        return (CompletableFuture<JsonObject>)api().getMethod("ticket",ServerPlayer.class,String.class,String.class,long.class,BooleanSupplier.class,Runnable.class,Runnable.class,boolean.class,String.class)
            .invoke(null,p,key,port,price,valid,issue,persist,refund,original);
    }
}
