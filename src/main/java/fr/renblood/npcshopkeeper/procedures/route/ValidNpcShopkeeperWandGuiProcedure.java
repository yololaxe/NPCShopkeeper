package fr.renblood.npcshopkeeper.procedures.route;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.level.Level;

import java.util.HashMap;

import static fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getDataSize;

public class ValidNpcShopkeeperWandGuiProcedure {
	public static void execute(Entity entity, HashMap guistate, String category, String name, String pointsText, String minTimerText, String maxTimerText) {
		if (entity == null || guistate == null)
			return;

		// Les valeurs sont maintenant passées en arguments, plus besoin de les chercher dans guistate
		// String name = guistate.containsKey("text:name") ? ((EditBox) guistate.get("text:name")).getValue() : "";
		// ...

		int maxPoints = getDataSize();

		// Vérifications des champs
		if (name == null || name.isEmpty()) {
			sendMessageToPlayer(entity, "Le champ Name est vide !");
			return;
		}

		int points;
		try {
			points = Integer.parseInt(pointsText);
			if (points <= 0 || points > maxPoints) {
				sendMessageToPlayer(entity, "Le champ Number of Points doit être > 0 et <= " + maxPoints + " !");
				return;
			}
		} catch (NumberFormatException e) {
			sendMessageToPlayer(entity, "Le champ Number of Points doit être un nombre !");
			return;
		}

		int minTimer;
		try {
			minTimer = Integer.parseInt(minTimerText);
			if (minTimer <= 0) {
				sendMessageToPlayer(entity, "Le champ Minimum Time doit être > 0 !");
				return;
			}
		} catch (NumberFormatException e) {
			sendMessageToPlayer(entity, "Le champ Minimum Time doit être un nombre !");
			return;
		}

		int maxTimer;
		try {
			maxTimer = Integer.parseInt(maxTimerText);
			if (maxTimer <= 0) {
				sendMessageToPlayer(entity, "Le champ Maximum Time doit être > 0 !");
				return;
			}
			if (minTimer > maxTimer) {
				sendMessageToPlayer(entity, "Le Minimum Time doit être <= Maximum Time !");
				return;
			}
		} catch (NumberFormatException e) {
			sendMessageToPlayer(entity, "Le champ Maximum Time doit être un nombre !");
			return;
		}

		if (entity instanceof ServerPlayer serverPlayer) {
			Level world = serverPlayer.level();
			PointDefiningModeProcedure.start(serverPlayer, world, points, minTimer, maxTimer, category, name);

			serverPlayer.closeContainer();
		}
	}

	private static void sendMessageToPlayer(Entity entity, String message) {
		if (entity instanceof Player _player && !_player.level().isClientSide()) {
			_player.displayClientMessage(Component.literal(message), false);
		}
	}
}
