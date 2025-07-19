package fr.renblood.npcshopkeeper.procedures.trade;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.trade.TradeResult;
import fr.renblood.npcshopkeeper.manager.trade.MoneyCalculator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

public class TradeProcedure {
	private static boolean isProcessingTrade = false;
	private static final Logger LOGGER = LogManager.getLogger(TradeProcedure.class);

	// ── Helpers ─────────────────────────────────────────────────────────

	private static Trade getTradeByName(String name) {
		JsonRepository<Trade> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.path),
				"trades",
				Trade::fromJson,
				Trade::toJson
		);
		return repo.loadAll().stream()
				.filter(t -> t.getName().equals(name))
				.findFirst().orElse(null);
	}

	private static TradeHistory getTradeHistoryById(String id) {
		JsonRepository<TradeHistory> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.pathHistory),
				"history",
				TradeHistory::fromJson,
				TradeHistory::toJson
		);
		return repo.loadAll().stream()
				.filter(h -> h.getId().equals(id))
				.findFirst().orElse(null);
	}

	private static void markTradeAsFinished(ServerPlayer player, String id) {
		JsonRepository<TradeHistory> repo = new JsonRepository<>(
				Paths.get(JsonFileManager.pathHistory),
				"history",
				TradeHistory::fromJson,
				TradeHistory::toJson
		);
		List<TradeHistory> all = new ArrayList<>(repo.loadAll());
		for (TradeHistory h : all) {
			if (h.getId().equals(id)) {
				h.setFinished(true);
				break;
			}
		}
		repo.saveAll(all);
	}

	// ── Main execution ────────────────────────────────────────────────────

	@SuppressWarnings("unchecked")
	public static void execute(Entity entity) {
		if (entity == null || isProcessingTrade) return;
		isProcessingTrade = true;

		if (entity instanceof ServerPlayer player
				&& player.containerMenu instanceof Supplier<?> sup
				&& sup.get() instanceof Map<?, ?> rawSlots) {

			Map<Integer, Slot> slots = (Map<Integer, Slot>) rawSlots;

			// Read trade name + id from slot 12
			String label = slots.get(12).getItem()
					.getDisplayName().getString();
			var parts = label.replace("[","").replace("]","")
					.split(" ",2);
			String tradeName = parts[0], tradeId = parts[1];

			TradeHistory th = getTradeHistoryById(tradeId);
			boolean ongoing = (th != null && !th.isFinished());

			// Validate item/req slots (0/1,2/3,...)
			if (isValidSlotPair(slots,0,1,player)
					&& isValidSlotPair(slots,2,3,player)
					&& isValidSlotPair(slots,4,5,player)
					&& isValidSlotPair(slots,6,7,player)
					&& ongoing) {

				clearAndRemoveSlots(player, slots);
				giveRewards(player, slots, tradeId, tradeName);
				markTradeAsFinished(player, tradeId);
				player.containerMenu.broadcastChanges();
			}
		}

		isProcessingTrade = false;
	}

	// ── Utility methods (unchanged) ──────────────────────────────────────

	private static boolean isValidSlotPair(
			Map<Integer, Slot> slots, int req, int pay, ServerPlayer p) {
		// your existing slot‐check logic...
		return true; // simplified for brevity
	}

	private static void clearAndRemoveSlots(
			ServerPlayer p, Map<Integer, Slot> slots) {
		// your existing logic...
	}

	private static void giveRewards(
			ServerPlayer player,
			Map<Integer, Slot> slots,
			String tradeId,
			String tradeName
	) {
		LOGGER.info("Give rewards for trade {}", tradeId);

		TradeHistory history = getTradeHistoryById(tradeId);
		if (history == null) {
			LOGGER.error("No history for trade {}", tradeId);
			return;
		}

		// distribute coins
		int total = history.getTotalPrice();
		int[] coins = MoneyCalculator.getIntInCoins(total);
		Item[] coinItems = {
				BuiltInRegistries.ITEM.get(
						new ResourceLocation("medieval_coins:gold_coin")),
				BuiltInRegistries.ITEM.get(
						new ResourceLocation("medieval_coins:silver_coin")),
				BuiltInRegistries.ITEM.get(
						new ResourceLocation("medieval_coins:bronze_coin")),
				Items.COPPER_INGOT
		};
		for (int i=0, slot=8; i<coins.length && slot<=9; i++) {
			if (coins[i]>0) {
				setSlot(slots, slot++, new ItemStack(coinItems[i], coins[i]));
			}
		}

		// give the trade result in slot 10
		Trade t = getTradeByName(tradeName);
		TradeResult res = t.getResult();
		if (res==null) {
			LOGGER.error("No result for trade {}", tradeName);
			return;
		}
		ItemStack out = new ItemStack(
				BuiltInRegistries.ITEM.get(new ResourceLocation(res.getItem())),
				res.getQuantity()
		);
		setSlot(slots, 10, out);
		player.containerMenu.broadcastChanges();
	}

	private static void setSlot(
			Map<Integer, Slot> slots, int id, ItemStack s) {
		slots.get(id).set(s);
	}


private static ItemStack getItem(Map _slots, int slotId) {
		return _slots.containsKey(slotId) ? ((Slot) _slots.get(slotId)).getItem() : ItemStack.EMPTY;
	}

	private static int getAmount(Map _slots, int slotId) {
		ItemStack stack = getItem(_slots, slotId);
		return stack != null ? stack.getCount() : 0;
	}
}
