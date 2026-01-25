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
							boolean isNameValid = checkTradeName(worldSavePath, tradeName);

							if (isNameValid) {
								// Si le nom est valide, écrire dans le fichier
								writeNewTradeToFile(worldSavePath, newTrade, _player);
								_player.displayClientMessage(Component.literal("Nouveau trade enregistré !"), true);
								_player.closeContainer(); // Fermer l'inventaire seulement si tout est correct
							} else {
								// Sinon, ne pas fermer et afficher un message d'erreur
								_player.displayClientMessage(Component.literal("Erreur : Un trade avec ce nom existe déjà."), false);
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

	public static void writeNewTradeToFile(String filePath, JsonObject newTrade, Player player) {
		try {
			FileReader reader = new FileReader(filePath);
			BufferedReader bufferedReader = new BufferedReader(reader);
			StringBuilder content = new StringBuilder();
			String line;

			// Lire le fichier ligne par ligne et vérifier s'il est vide
			while ((line = bufferedReader.readLine()) != null) {
				content.append(line);
			}
			reader.close();

			JsonObject fileObject;
			if (content.length() == 0) {
				// Si le fichier est vide, créer une nouvelle structure JSON
				fileObject = new JsonObject();
				fileObject.add("trades", new JsonArray()); // Ajouter un tableau "trades" vide
			} else {
				// Si le fichier n'est pas vide, parser son contenu
				JsonElement fileElement = JsonParser.parseString(content.toString());

				// Si le contenu n'est pas un objet JSON valide, créer une nouvelle structure
				if (!fileElement.isJsonObject()) {
					fileObject = new JsonObject();
					fileObject.add("trades", new JsonArray()); // Ajouter un tableau "trades" vide
				} else {
					fileObject = fileElement.getAsJsonObject();
				}
			}

			// Ajouter le nouveau trade au fichier JSON
			JsonArray tradeArray = fileObject.getAsJsonArray("trades");
			tradeArray.add(newTrade);

			// Écrire le fichier JSON mis à jour dans le fichier
			FileWriter writer = new FileWriter(filePath);
			writer.write(fileObject.toString());
			writer.close();

			// Informer le joueur de la réussite
			player.displayClientMessage(Component.literal("Nouveau trade enregistré !"), false);

		} catch (IOException e) {
			player.displayClientMessage(Component.literal("Erreur lors de la lecture ou de l'écriture du fichier JSON: " + e.getMessage()), false);
			e.printStackTrace();
		}
	}
}
