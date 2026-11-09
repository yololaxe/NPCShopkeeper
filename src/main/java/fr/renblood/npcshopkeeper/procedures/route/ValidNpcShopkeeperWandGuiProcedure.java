package fr.renblood.npcshopkeeper.procedures.route;

import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;

import static fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager.getDataSize;

public class ValidNpcShopkeeperWandGuiProcedure {
	public static void execute(Entity entity, HashMap guistate, String roadTypeId, String category, String name,
						   String pointsText, String minTimerText, String maxTimerText) {
		if (entity == null || guistate == null)
			return;

		// Les valeurs sont maintenant passées en arguments, plus besoin de les chercher dans guistate
		// String name = guistate.containsKey("text:name") ? ((EditBox) guistate.get("text:name")).getValue() : "";
		// ...

		int maxPoints = 32;
		CommercialRoad.RoadType roadType = CommercialRoad.RoadType.fromId(roadTypeId);
		String resolvedCategory = roadType == CommercialRoad.RoadType.INN_MENU ? "inn_menu" : resolveTradeCategory(category);
		if (roadType == CommercialRoad.RoadType.TRADE && resolvedCategory == null) {
			String available = RuntimeDataCache.getTrades().stream()
					.map(trade -> trade.getCategory())
					.filter(value -> value != null && !value.isBlank())
					.distinct().sorted(String.CASE_INSENSITIVE_ORDER).limit(8)
					.reduce((left, right) -> left + ", " + right).orElse("aucune");
			sendMessageToPlayer(entity, "Categorie de trade inconnue. Disponibles: " + available);
			return;
		}

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
			PointDefiningModeProcedure.start(serverPlayer, world, points, minTimer, maxTimer, resolvedCategory, name, roadType);

			serverPlayer.closeContainer();
		}
	}

	private static String resolveTradeCategory(String requested) {
		String normalized = normalize(requested);
		return RuntimeDataCache.getTrades().stream()
				.map(trade -> trade.getCategory())
				.filter(value -> value != null && normalize(value).equals(normalized))
				.findFirst().orElse(null);
	}

	private static String normalize(String value) {
		if (value == null) return "";
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		int separator = normalized.indexOf(':');
		return separator >= 0 ? normalized.substring(separator + 1) : normalized;
	}

	private static void sendMessageToPlayer(Entity entity, String message) {
		if (entity instanceof Player _player && !_player.level().isClientSide()) {
			_player.displayClientMessage(Component.literal(message), false);
		}
	}
}
