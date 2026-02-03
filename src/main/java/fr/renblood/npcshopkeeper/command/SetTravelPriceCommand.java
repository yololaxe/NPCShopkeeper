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

@Mod.EventBusSubscriber
public class SetTravelPriceCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_travel_config")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("price")
                        .then(Commands.argument("blocks_per_iron", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "blocks_per_iron");
                                    PortManager.setBlocksPerIron(value);
                                    syncConfig();
                                    context.getSource().sendSuccess(() -> Component.literal("✅ Prix du voyage mis à jour : 1 Fer pour " + value + " blocs."), true);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("day_length")
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                .executes(context -> {
                                    int value = IntegerArgumentType.getInteger(context, "minutes");
                                    PortManager.setDayLengthInMinutes(value);
                                    syncConfig();
                                    context.getSource().sendSuccess(() -> Component.literal("✅ Durée du jour configurée à " + value + " minutes."), true);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void syncConfig() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), 
            new SyncHarborConfigPacket(PortManager.getBlocksPerIron(), PortManager.getDayLengthInMinutes()));
    }
}
