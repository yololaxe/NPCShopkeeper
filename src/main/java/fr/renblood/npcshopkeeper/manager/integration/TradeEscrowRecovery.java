package fr.renblood.npcshopkeeper.manager.integration;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="npcshopkeeper")
public final class TradeEscrowRecovery {
    public static final String KEY="RenbloodTradeEscrow";
    public static void persist(ServerPlayer player){
        try{Class.forName("fr.renblood.medievalcoins.bank.BankPlayerSnapshot").getMethod("save",ServerPlayer.class).invoke(null,player);}
        catch(Exception failure){org.slf4j.LoggerFactory.getLogger("RenbloodEconomy").warn("Sauvegarde du retour d’objets à vérifier pour {}",player.getUUID(),failure);}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        ListTag stored=player.getPersistentData().getList(KEY,10),left=new ListTag();
        for(int i=0;i<stored.size();i++){
            CompoundTag row=stored.getCompound(i);ItemStack stack=ItemStack.of(row);
            player.getInventory().add(stack);
            if(!stack.isEmpty()){CompoundTag remaining=stack.save(new CompoundTag());remaining.putInt("Slot",row.getInt("Slot"));left.add(remaining);}
        }
        player.getPersistentData().put(KEY,left);
        if(!stored.isEmpty())persist(player);
        if(!left.isEmpty())player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Des objets de votre échange sont conservés. Libérez de la place, puis rouvrez un échange pour les reprendre."));
    }
}
