package fr.renblood.npcshopkeeper.services;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record ServiceResponse(String action,String json){
    public ServiceResponse(FriendlyByteBuf b){this(b.readUtf(40),b.readUtf(1048576));}
    public void encode(FriendlyByteBuf b){b.writeUtf(action,40);b.writeUtf(json,1048576);}
    public static void handle(ServiceResponse m,Supplier<NetworkEvent.Context> s){var c=s.get();c.enqueueWork(()->ServiceClient.receive(m.action(),m.json()));c.setPacketHandled(true);}
}
