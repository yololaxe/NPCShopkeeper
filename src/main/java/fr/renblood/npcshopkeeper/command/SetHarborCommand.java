package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.init.EntityInit;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class SetHarborCommand {
    private static final Logger LOGGER = LogManager.getLogger(SetHarborCommand.class);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_harbor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> {
                            try {
                                String portName = StringArgumentType.getString(context, "name");
                                CommandSourceStack source = context.getSource();
                                ServerLevel world = source.getLevel();
                                BlockPos pos = BlockPos.containing(source.getPosition());

                                // Vérification : Un capitaine existe-t-il déjà à proximité ?
                                AABB checkArea = new AABB(pos).inflate(2);
                                boolean alreadyExists = world.getEntitiesOfClass(TradeNpcEntity.class, checkArea,
                                        e -> e.isCaptain() && e.getPortName().equals(portName)).size() > 0;

                                if (alreadyExists) {
                                    source.sendFailure(Component.translatable("command.npcshopkeeper.set_harbor.already_exists", portName));
                                    return 0;
                                }

                                // 1. Faire spawn l'entité Capitaine
                                TradeNpcEntity npc = new TradeNpcEntity(EntityInit.TRADE_NPC_ENTITY.get(), world);
                                npc.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                                
                                // Configuration spécifique Capitaine (NBT uniquement)
                                npc.setCaptain(true);
                                npc.setPortName(portName);
                                
                                world.addFreshEntity(npc);
                                LOGGER.info("Capitaine spawn pour le port: {}", portName);

                                // 2. Enregistrer le port
                                Port port = new Port(
                                        portName,
                                        pos,
                                        world.dimension().location().toString()
                                );
                                PortManager.addPort(port);
                                LOGGER.info("Port enregistré: {}", portName);

                                source.sendSuccess(() -> Component.translatable("command.npcshopkeeper.set_harbor.success", portName), true);
                                return 1;
                            } catch (Exception e) {
                                LOGGER.error("Erreur critique dans SetHarborCommand", e);
                                context.getSource().sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
                                return 0;
                            }
                        })
                )
        );
    }
}
