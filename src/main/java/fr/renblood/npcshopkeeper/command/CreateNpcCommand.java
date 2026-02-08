package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import fr.renblood.npcshopkeeper.world.inventory.CreateNpcMenu;
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

@Mod.EventBusSubscriber
public class CreateNpcCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("createnpc")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    try {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        openGui(player);
                        return 1;
                    } catch (Exception e) {
                        context.getSource().sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
                        return 0;
                    }
                })
        );
    }

    public static void openGui(ServerPlayer player) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.npcshopkeeper.create_npc.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new CreateNpcMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()));
            }
        });
    }
}
