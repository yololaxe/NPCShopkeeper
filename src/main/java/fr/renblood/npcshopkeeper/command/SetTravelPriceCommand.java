package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.SyncHarborConfigPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

public class SetTravelPriceCommand {

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_travel_config")
                .requires(CommandPermissions::isAdmin)
                .then(Commands.literal("price")
                        .then(Commands.argument("blocks_per_iron", IntegerArgumentType.integer(1))
                                .executes(context -> setPrice(context.getSource(), IntegerArgumentType.getInteger(context, "blocks_per_iron")))
                        )
                )
                .then(Commands.literal("day_length")
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                .executes(context -> setDayLength(context.getSource(), IntegerArgumentType.getInteger(context, "minutes")))
                        )
                )
        );
    }

    public static int setPrice(CommandSourceStack source, int value) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }
        PortManager.setBlocksPerIron(value);
        syncConfig();
        source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.set_travel_config.price_success", value), true);
        return 1;
    }

    public static int setDayLength(CommandSourceStack source, int value) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }
        PortManager.setDayLengthInMinutes(value);
        syncConfig();
        source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.set_travel_config.day_length_success", value), true);
        return 1;
    }

    private static void syncConfig() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), 
            new SyncHarborConfigPacket(PortManager.getBlocksPerIron(), PortManager.getDayLengthInMinutes()));
    }
}
