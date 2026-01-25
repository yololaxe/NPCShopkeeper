package fr.renblood.npcshopkeeper.network;


import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.procedures.route.ValidNpcShopkeeperWandGuiProcedure;
import fr.renblood.npcshopkeeper.world.inventory.NpcShopkeeperWandGuiMenu;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;


import java.util.function.Supplier;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class NpcShopkeeperWandGuiButtonMessage {
	private final int buttonID, x, y, z;
	private final String category;
	private final String name;
	private final String points;
	private final String minTimer;
	private final String maxTimer;


	// Constructeur pour désérialisation
	public NpcShopkeeperWandGuiButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.category = buffer.readUtf(32767);
		this.name = buffer.readUtf(32767);
		this.points = buffer.readUtf(32767);
		this.minTimer = buffer.readUtf(32767);
		this.maxTimer = buffer.readUtf(32767);
	}

	// Constructeur pour création
	public NpcShopkeeperWandGuiButtonMessage(int buttonID, int x, int y, int z, String category, String name, String points, String minTimer, String maxTimer) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.category = category;
		this.name = name;
		this.points = points;
		this.minTimer = minTimer;
		this.maxTimer = maxTimer;
	}

	// Sérialisation
	public static void buffer(NpcShopkeeperWandGuiButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeUtf(message.category);
		buffer.writeUtf(message.name);
		buffer.writeUtf(message.points);
		buffer.writeUtf(message.minTimer);
		buffer.writeUtf(message.maxTimer);
	}

	// Désérialisation
	public static void handler(NpcShopkeeperWandGuiButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			handleButtonAction(entity, message.buttonID, message.x, message.y, message.z, message.category, message.name, message.points, message.minTimer, message.maxTimer);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, String category, String name, String points, String minTimer, String maxTimer) {
		Level world = entity.level();
		if (!world.hasChunkAt(new BlockPos(x, y, z))) return;

		if (buttonID == 0) {
			// On passe toutes les valeurs reçues à la procédure
			ValidNpcShopkeeperWandGuiProcedure.execute(entity, NpcShopkeeperWandGuiMenu.guistate, category, name, points, minTimer, maxTimer);
		}
	}
	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		Npcshopkeeper.addNetworkMessage(
				NpcShopkeeperWandGuiButtonMessage.class,
				NpcShopkeeperWandGuiButtonMessage::buffer,
				NpcShopkeeperWandGuiButtonMessage::new,
				NpcShopkeeperWandGuiButtonMessage::handler
		);
	}
}
