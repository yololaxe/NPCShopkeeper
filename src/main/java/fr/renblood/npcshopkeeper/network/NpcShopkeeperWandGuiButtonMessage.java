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


	// Constructeur pour désérialisation
	public NpcShopkeeperWandGuiButtonMessage(FriendlyByteBuf buffer) {
		this.buttonID = buffer.readInt();
		this.x = buffer.readInt();
		this.y = buffer.readInt();
		this.z = buffer.readInt();
		this.category = buffer.readUtf(32767); // Lire la catégorie
	}

	// Constructeur pour création
	public NpcShopkeeperWandGuiButtonMessage(int buttonID, int x, int y, int z, String category) {
		this.buttonID = buttonID;
		this.x = x;
		this.y = y;
		this.z = z;
		this.category = category; // Initialiser la catégorie
	}

	// Sérialisation
	public static void buffer(NpcShopkeeperWandGuiButtonMessage message, FriendlyByteBuf buffer) {
		buffer.writeInt(message.buttonID);
		buffer.writeInt(message.x);
		buffer.writeInt(message.y);
		buffer.writeInt(message.z);
		buffer.writeUtf(message.category); // Écrire la catégorie
	}

	// Désérialisation
	public static void handler(NpcShopkeeperWandGuiButtonMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			Player entity = context.getSender();
			handleButtonAction(entity, message.buttonID, message.x, message.y, message.z, message.category);
		});
		context.setPacketHandled(true);
	}

	public static void handleButtonAction(Player entity, int buttonID, int x, int y, int z, String category) {
		Level world = entity.level();
		if (!world.hasChunkAt(new BlockPos(x, y, z))) return;

		// Appeler la procédure associée
		if (buttonID == 0) {
			ValidNpcShopkeeperWandGuiProcedure.execute(entity, NpcShopkeeperWandGuiMenu.guistate, category);
		}
	}
	@SubscribeEvent
	public static void registerMessage(FMLCommonSetupEvent event) {
		Npcshopkeeper.PACKET_HANDLER.registerMessage(
				0, // ID du message
				NpcShopkeeperWandGuiButtonMessage.class,
				NpcShopkeeperWandGuiButtonMessage::buffer,
				NpcShopkeeperWandGuiButtonMessage::new,
				NpcShopkeeperWandGuiButtonMessage::handler
		);
	}
}


