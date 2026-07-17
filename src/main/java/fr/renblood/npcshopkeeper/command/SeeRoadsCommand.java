package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.SyncRoadsPacket;
import fr.renblood.npcshopkeeper.world.inventory.SeeRoadsMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

public class SeeRoadsCommand {
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seeroads")
                .requires(CommandPermissions::isAdmin)
                .executes(context -> execute(context.getSource()))
        );
    }

    public static int execute(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }
        ServerPlayer player = source.getPlayerOrException();
        openGui(player, 0);
        return 1;
    }

    public static void openGui(ServerPlayer player, int page) {
        // 1. Synchroniser les routes avec le client
        PacketHandler.sendToPlayer(new SyncRoadsPacket(Npcshopkeeper.COMMERCIAL_ROADS), player);

        // 2. Ouvrir le GUI
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.npcshopkeeper.see_roads.title", (page + 1));
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeInt(page);
                return new SeeRoadsMenu(id, inventory, buf);
            }
        }, buf -> buf.writeInt(page));
    }
}
