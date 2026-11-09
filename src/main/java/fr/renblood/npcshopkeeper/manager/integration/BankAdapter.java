package fr.renblood.npcshopkeeper.manager.integration;
import net.minecraft.server.level.ServerPlayer;
import java.util.concurrent.CompletableFuture;
public final class BankAdapter {
 private BankAdapter(){}
 public static CompletableFuture<Long> credit(ServerPlayer player,String operationKey,long amount,String reason){return call("creditBank",player,operationKey,amount,reason);}
 public static CompletableFuture<Long> debit(ServerPlayer player,String operationKey,long amount,String reason){return call("debitBank",player,operationKey,amount,reason);}
 @SuppressWarnings("unchecked") private static CompletableFuture<Long> call(String method,ServerPlayer player,String key,long amount,String reason){
  try{
   if(amount<=0||key==null||key.isBlank()||reason==null||reason.isBlank())throw new IllegalArgumentException("Montant, identifiant stable et motif requis");
   var api=Class.forName("fr.renblood.medievalcoins.api.MedievalCoinsAPI");
   return (CompletableFuture<Long>)api.getMethod(method,ServerPlayer.class,String.class,long.class,String.class).invoke(null,player,key,amount,reason);
  }catch(Exception error){return CompletableFuture.failedFuture(error);}
 }
}
