package fr.renblood.npcshopkeeper.services;
import com.google.gson.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record ServiceRequest(String action,String json) {
    public ServiceRequest(FriendlyByteBuf b){this(b.readUtf(40),b.readUtf(131072));}
    public void encode(FriendlyByteBuf b){b.writeUtf(action,40);b.writeUtf(json,131072);}
    public static void handle(ServiceRequest m,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->{
        if(c.getSender()==null)return;
        try{WorkshopServer.handle(c.getSender(),m.action(),JsonParser.parseString(m.json()).getAsJsonObject());}
        catch(Exception failure){WorkshopServer.error(c.getSender(),"Demande invalide.");}
    });c.setPacketHandled(true);}
}
