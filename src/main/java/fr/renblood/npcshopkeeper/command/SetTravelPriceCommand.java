package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SetTravelPriceCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_travel_price")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("blocks_per_iron", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int value = IntegerArgumentType.getInteger(context, "blocks_per_iron");
                            PortManager.setBlocksPerIron(value);
                            context.getSource().sendSuccess(() -> Component.literal("✅ Prix du voyage mis à jour : 1 Fer pour " + value + " blocs."), true);
                            return 1;
                        })
                )
        );
    }
}
