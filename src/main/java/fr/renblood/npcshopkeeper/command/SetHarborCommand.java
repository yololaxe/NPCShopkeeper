package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class SetHarborCommand {
    private static final Logger LOGGER = LogManager.getLogger(SetHarborCommand.class);

    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_harbor")
                .requires(CommandPermissions::isAdmin)
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> execute(context.getSource(), StringArgumentType.getString(context, "name")))));
    }

    public static int execute(CommandSourceStack source, String portName) {
        if (!CommandPermissions.isAdmin(source)) {
            source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
            return 0;
        }

        try {
            ServerLevel world = source.getLevel();
            BlockPos pos = BlockPos.containing(source.getPosition());

            AABB checkArea = new AABB(pos).inflate(2);
            boolean alreadyExists = !world.getEntitiesOfClass(TradeNpcEntity.class, checkArea,
                    e -> e.isCaptain() && e.getPortName().equals(portName)).isEmpty();
            if (alreadyExists) {
                source.sendFailure(Component.translatable("command.npcshopkeeper.set_harbor.already_exists", portName));
                return 0;
            }

            TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
            npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            npc.setCaptain(true);
            npc.setPortName(portName);
            world.addFreshEntity(npc);

            Port port = new Port(portName, pos, world.dimension().location().toString());
            PortManager.addPort(port);

            NpcSpawn spawn = new NpcSpawn();
            spawn.spawn_id = UUID.randomUUID().toString();
            spawn.npc_id = "captain_" + portName;
            spawn.world = world.dimension().location().toString();
            spawn.x = pos.getX();
            spawn.y = pos.getY();
            spawn.z = pos.getZ();
            spawn.spawn_rule = "STATIC";
            spawn.active = true;
            MedievalCoinsIntegration.createNpcSpawn(spawn);

            LOGGER.info("Port '{}' cree en {}", portName, pos);
            source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.set_harbor.success", portName), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Erreur critique dans SetHarborCommand", e);
            source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
            return 0;
        }
    }
}
