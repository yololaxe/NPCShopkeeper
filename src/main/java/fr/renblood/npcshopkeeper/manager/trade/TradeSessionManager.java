package fr.renblood.npcshopkeeper.manager.trade;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TradeSessionManager {
    private static final Logger LOGGER = LogManager.getLogger(TradeSessionManager.class);
    private static final Map<UUID, String> activeTrades = new HashMap<>();

    public static void setTradeId(UUID playerUuid, String tradeId) {
        LOGGER.info("TradeSessionManager: Setting trade ID {} for player {}", tradeId, playerUuid);
        activeTrades.put(playerUuid, tradeId);
    }

    public static String getTradeId(UUID playerUuid) {
        String id = activeTrades.get(playerUuid);
        LOGGER.info("TradeSessionManager: Getting trade ID for player {}: {}", playerUuid, id);
        return id;
    }

    public static void clearTradeId(UUID playerUuid) {
        LOGGER.info("TradeSessionManager: Clearing trade ID for player {}", playerUuid);
        activeTrades.remove(playerUuid);
    }
}
