package fr.renblood.npcshopkeeper.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

final class CommandPermissions {
    private static final int ADMIN_PERMISSION = 2;

    private CommandPermissions() {
    }

    static boolean isAdmin(CommandSourceStack source) {
        if (source.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            return source.getServer().isSingleplayerOwner(player.getGameProfile());
        }

        return false;
    }
}
