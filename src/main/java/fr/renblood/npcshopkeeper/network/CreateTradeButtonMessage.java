package fr.renblood.npcshopkeeper.network;


import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.procedures.trade.CreateTradeValidationProcedure;
import fr.renblood.npcshopkeeper.world.inventory.CreateTradeMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;




import java.util.function.Supplier;
import java.util.HashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreateTradeButtonMessage {
	private final int buttonID, x, y, z;
	private final String tradeName; // Ajout du nom du trade

	public CreateTradeButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.tradeName = buffer.readUtf(); // Lecture du nom
	}

	public CreateTradeButtonMessage(int buttonID, int x, int y, int z, String tradeName) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.tradeName = tradeName;
	}

	public static void buffer(CreateTradeButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeUtf(message.tradeName); // Écriture du nom
	}

	public static void handler(CreateTradeButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			int buttonID = message.buttonID;
			int x = message.x;
			int y = message.y;
			int z = message.z;
			String tradeName = message.tradeName;
			handleButtonAction(entity, buttonID, x, y, z, tradeName);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, String tradeName) {
		Level world = entity.level();
		HashMap guistate = CreateTradeMenu.guistate;
		// security measure to prevent arbitrary chunk generation
		if (!world.hasChunkAt(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {
			// On passe le tradeName directement à la procédure
			CreateTradeValidationProcedure.execute(entity, guistate, tradeName);
		}
	}

	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		Npcshopkeeper.addNetworkMessage(CreateTradeButtonMessage.class, CreateTradeButtonMessage::buffer, CreateTradeButtonMessage::new, CreateTradeButtonMessage::handler);
	}
}
