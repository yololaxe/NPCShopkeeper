package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import java.util.Locale;
import java.util.Objects;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;


public class TradeCommand {
	public static void registerCommand(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
				Commands.literal("trade")
						.requires(CommandPermissions::isAdmin)
						.then(Commands.argument("name", StringArgumentType.string())
								.suggests(TRADE_SUGGESTIONS)
								.executes(ctx -> execute(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
						)
		);
	}

	public static int execute(CommandSourceStack source, String inputName) {
		if (!CommandPermissions.isAdmin(source)) {
			source.sendFailure(Component.translatable("command.npcshopkeeper.permission.denied"));
			return 0;
		}

		try {
			Level world = source.getLevel();
			double x = source.getPosition().x();
			double y = source.getPosition().y();
			double z = source.getPosition().z();
			Entity entity = source.getEntity();
			if (entity == null && world instanceof ServerLevel sl)
				entity = FakePlayerFactory.getMinecraft(sl);
			Direction direction = entity != null ? entity.getDirection() : Direction.DOWN;
			TradeCommandProcedure.execute(world, x, y, z, inputName, entity, "", "");
			return 1;
		} catch (Exception e) {
			source.sendFailure(Component.translatable("command.npcshopkeeper.error.internal", e.getMessage()));
			return 0;
		}
	}

    static final SuggestionProvider<CommandSourceStack> TRADE_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        String prefix = remaining.startsWith("\"") ? remaining.substring(1) : remaining;
        RuntimeDataCache.getTrades().stream().map(Trade::getName).filter(Objects::nonNull).distinct()
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).limit(50)
                .map(StringArgumentType::escapeIfRequired).forEach(builder::suggest);
        return builder.buildFuture();
    };
}
