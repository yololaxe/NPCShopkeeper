package fr.renblood.npcshopkeeper.command;

import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.SyncPortsPacket;
import fr.renblood.npcshopkeeper.world.inventory.TravelMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

public class OpenTravelGuiCommand {
    public static void openGui(ServerPlayer player, String currentPortName) {
        // 1. Synchroniser les ports avec le client
        PacketHandler.sendToPlayer(new SyncPortsPacket(PortManager.getAllPorts()), player);

        // 2. Ouvrir le GUI
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Voyage depuis " + currentPortName);
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf(currentPortName);
                return new TravelMenu(id, inventory, buf);
            }
        }, buf -> buf.writeUtf(currentPortName));
    }
}
