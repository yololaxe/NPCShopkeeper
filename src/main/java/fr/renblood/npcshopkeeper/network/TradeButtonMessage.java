package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.procedures.trade.CancelProcedure;
import fr.renblood.npcshopkeeper.procedures.trade.TradeProcedure;
import fr.renblood.npcshopkeeper.world.inventory.TradeMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class TradeButtonMessage {
    private static final Logger LOGGER = LogManager.getLogger(TradeButtonMessage.class);
    private final int buttonID, x, y, z;

    public TradeButtonMessage(FriendlyByteBuf buffer) {
        this.buttonID = buffer.readInt();
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.z = buffer.readInt();
    }

    public TradeButtonMessage(int buttonID, int x, int y, int z) {
        this.buttonID = buttonID;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void buffer(TradeButtonMessage message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.buttonID);
        buffer.writeInt(message.x);
        buffer.writeInt(message.y);
        buffer.writeInt(message.z);
    }

    public static void handler(TradeButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Player entity = context.getSender();
            int buttonID = message.buttonID;
            int x = message.x;
            int y = message.y;
            int z = message.z;
            handleButtonAction(entity, buttonID, x, y, z);
        });
        context.setPacketHandled(true);
    }

    public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z) {
        Level world = entity.level();
        
        // security measure to prevent arbitrary chunk generation
        if (!world.hasChunkAt(new BlockPos(x, y, z)))
            return;
            
        Npcshopkeeper.debugLog(LOGGER, "TradeButtonMessage received. ButtonID: " + buttonID + ", Container ID: " + entity.containerMenu.containerId);
        
        if (!(entity.containerMenu instanceof TradeMenu)) {
            LOGGER.warn("Player " + entity.getName().getString() + " tried to use TradeButton but container is not TradeMenu! (Container: " + entity.containerMenu.getClass().getName() + ")");
            return;
        }

        if (buttonID == 0) {
            TradeProcedure.execute(entity);
        }
        if (buttonID == 1) {
            CancelProcedure.execute(entity);
        }
    }

    @SubscribeEvent
    public static void registerMessage(FMLCommonSetupEvent event) {
        Npcshopkeeper.addNetworkMessage(TradeButtonMessage.class, TradeButtonMessage::buffer, TradeButtonMessage::new, TradeButtonMessage::handler);
    }
}
