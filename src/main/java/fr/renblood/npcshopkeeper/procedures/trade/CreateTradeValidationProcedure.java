package fr.renblood.npcshopkeeper.procedures.trade;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class CreateTradeValidationProcedure {
	public static void execute(Entity entity, HashMap guistate, String tradeName) {
		if (entity == null || guistate == null)
			return;

		final ItemStack[] trades = new ItemStack[8];
		final int[] trades_quantity = new int[8];
		ItemStack categoryItem = null;
		ItemStack resultItem = null;
		int resultQuantity = 0;

		// Récupération de tous les trades d'un coup
		if (entity instanceof Player _player && _player.containerMenu instanceof Supplier _current && _current.get() instanceof Map _slots) {
			for (int i = 0; i < 8; i++) {
				ItemStack item = ((Slot) _slots.get(i)).getItem();
				if (!item.isEmpty() && item.getItem() != Items.AIR) {
					trades[i] = item;
					trades_quantity[i] = item.getCount();
				} else {
					trades[i] = null;  // Si l'item est minecraft:air ou vide
				}
			}

			resultItem = ((Slot) _slots.get(10)).getItem();
			if (resultItem != null && !resultItem.isEmpty() && resultItem.getItem() != Items.AIR) {
				resultQuantity = resultItem.getCount();
			} else {
				resultItem = null;  // Si le slot est vide ou contient de l'air
			}

			categoryItem = ((Slot) _slots.get(11)).getItem();

			if (categoryItem != null && !categoryItem.isEmpty() && categoryItem.getItem() != Items.AIR){
				// Utilisation du tradeName passé en paramètre au lieu de guistate
				if (tradeName != null && !tradeName.isEmpty()) {
					if (trades[0] != null && trades[1] != null) { // On vérifie qu'il y a au moins un trade
						// _player.displayClientMessage(Component.literal(BuiltInRegistries.ITEM.getKey(trades[0].getItem()).toString()), false); // Debug spam

						JsonObject newTrade = new JsonObject();
						newTrade.addProperty("Name", tradeName);
						newTrade.addProperty("Category", BuiltInRegistries.ITEM.getKey(categoryItem.getItem()).toString()); // Catégorie du premier item

						// Construction des trades dans le JSON
						JsonArray tradesArray = new JsonArray();
						for (int i = 0; i < 8; i += 2) {
							if (trades[i] != null) {  // Ne pas ajouter si l'item est null
								JsonObject tradeItem = new JsonObject();
								tradeItem.addProperty("item", BuiltInRegistries.ITEM.getKey(trades[i].getItem()).toString());
								tradeItem.addProperty("min", trades_quantity[i]);  // Quantité min
								tradeItem.addProperty("max", trades_quantity[i + 1]);  // Quantité max
								tradesArray.add(tradeItem);
							}
						}

						newTrade.add("trades", tradesArray);
						if (resultItem != null) {
							JsonObject result = new JsonObject();
							result.addProperty("item", BuiltInRegistries.ITEM.getKey(resultItem.getItem()).toString());
							result.addProperty("quantity", resultQuantity);
							newTrade.add("result", result);
						} else {
							newTrade.add("result", null);  // Pas de résultat
						}

						// Obtenir le chemin du fichier de trades et vérifier le nom
						MinecraftServer server = _player.getServer();
						if (server != null) {
							String worldSavePath = server.getWorldPath(LevelResource.ROOT).resolve("npcshopkeeper/trade.json").toString();
                            if (writeNewTradeToFile(worldSavePath, newTrade, _player)) {
                                _player.displayClientMessage(Component.literal("Nouvelle offre enregistrée.")
                                        .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                                _player.closeContainer();
                            }
						}
					} else {
						if (!_player.level().isClientSide())
							_player.displayClientMessage(Component.literal("At least one trade required"), true);
					}
				} else {
					if (!_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("Name is required"), true);
				}
			} else {
				_player.displayClientMessage(Component.literal("Need a category"), true);
			}
		}
	}

	// Méthode pour vérifier si le nom existe déjà dans le fichier JSON
	public static boolean checkTradeName(String filePath, String newTradeName) {
		try {
			FileReader reader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(reader);
			StringBuilder content = new StringBuilder();
			String line;

			// Lire le fichier ligne par ligne
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line);
			}
			reader.close();

			// Si le fichier n'est pas vide, parser son contenu
			if (content.length() > 0) {
				JsonElement fileElement = JsonParser.parseString(content.toString());
				if (fileElement.isJsonObject()) {
					JsonObject fileObject = fileElement.getAsJsonObject();
					JsonArray tradeArray = fileObject.getAsJsonArray("trades");

					// Vérifier si le nom existe déjà dans les trades
					for (JsonElement tradeElement : tradeArray) {
						JsonObject tradeObject = tradeElement.getAsJsonObject();
						String existingTradeName = tradeObject.get("Name").getAsString();
						if (existingTradeName.equalsIgnoreCase(newTradeName)) {
							return false;  // Nom déjà utilisé
						}
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return true;  // Nom valide si non trouvé
	}

    public static boolean writeNewTradeToFile(String filePath, JsonObject newTrade, Player player) {
        try {
            fr.renblood.npcshopkeeper.data.io.JsonFileManager.updateJsonFile(filePath, root -> {
                if (!root.has("trades")) root.add("trades", new JsonArray());
                if (!root.get("trades").isJsonArray()) throw new IllegalArgumentException("Le catalogue doit être vérifié par l’équipe.");
                JsonArray trades = root.getAsJsonArray("trades");
                for (JsonElement row : trades) {
                    if (row.isJsonObject() && row.getAsJsonObject().has("Name")
                            && row.getAsJsonObject().get("Name").getAsString().equalsIgnoreCase(newTrade.get("Name").getAsString()))
                        throw new IllegalArgumentException("Une offre porte déjà ce nom.");
                }
                trades.add(newTrade.deepCopy());
            });
            return true;
        } catch (IOException failure) {
            org.apache.logging.log4j.LogManager.getLogger(CreateTradeValidationProcedure.class).error("Offre non sauvegardée", failure);
            player.displayClientMessage(Component.literal("Enregistrement impossible. Le catalogue doit être vérifié par l’équipe.")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
        } catch (RuntimeException failure) {
            player.displayClientMessage(Component.literal(failure instanceof IllegalArgumentException ? failure.getMessage()
                    : "Enregistrement non confirmé. Demande à l’équipe de vérifier le catalogue.")
                    .withStyle(net.minecraft.ChatFormatting.RED), false);
        }
        return false;
    }
}
