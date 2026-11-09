package fr.renblood.npcshopkeeper.manager.data;

import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;

import java.nio.file.Paths;
import java.util.*;

/** Indexes are built in full before publication and follow the repository snapshot identity. */
public final class RuntimeDataCache {
    private static NpcIndex npcs;
    private static TradeIndex trades;
    private static HistoryIndex histories;
    private static long indexBuilds;

    private RuntimeDataCache() { }

    public static void reloadAll() { reloadNpcs(); reloadTrades(); }
    public static synchronized void reset() { npcs = null; trades = null; histories = null; indexBuilds = 0; }
    public static synchronized long indexBuildCount() { return indexBuilds; }

    public static synchronized void reloadNpcs() {
        if (OnServerStartedManager.PATH_NPCS == null) return;
        JsonRepository.invalidate(Paths.get(OnServerStartedManager.PATH_NPCS));
        npcIndex();
    }
    public static synchronized void reloadTrades() {
        if (OnServerStartedManager.PATH == null) return;
        JsonRepository.invalidate(Paths.get(OnServerStartedManager.PATH));
        tradeIndex();
    }

    public static TradeNpc getNpc(UUID id) { return id == null ? null : npcIndex().byId.get(id); }
    public static Collection<TradeNpc> getNpcs() { return npcIndex().rows; }
    public static Trade getTrade(String name) { return name == null ? null : tradeIndex().byName.get(name); }
    public static Collection<Trade> getTrades() { return tradeIndex().rows; }
    public static List<Trade> getTradesByCategory(String category) {
        return tradeIndex().byCategory.getOrDefault(categoryKey(category), List.of());
    }
    public static TradeHistory getHistory(String id) { return id == null ? null : historyIndex().byId.get(id); }
    public static Optional<TradeHistory> getOpenHistory(String npcId) {
        if (npcId == null) return Optional.empty();
        return historyIndex().byNpc.getOrDefault(npcId, List.of()).stream()
                .filter(row -> !row.isFinished() && !row.isAbandoned()).findFirst();
    }
    public static int npcCount() { return npcIndex().rows.size(); }
    public static int tradeCount() { return tradeIndex().rows.size(); }

    private static String categoryKey(String category) { return category == null ? "" : category.toLowerCase(Locale.ROOT); }
    private static String pathKey(String path) { return path == null ? "" : Paths.get(path).toAbsolutePath().normalize().toString(); }

    private static synchronized NpcIndex npcIndex() {
        String path = OnServerStartedManager.PATH_NPCS;
        List<TradeNpc> source = path == null ? List.of() : new JsonRepository<>(Paths.get(path), "npcs", TradeNpc::fromJson, TradeNpc::toJson).snapshot();
        String key = pathKey(path);
        if (npcs == null || npcs.source != source || !npcs.path.equals(key)) {
            Map<UUID, TradeNpc> byId = new LinkedHashMap<>();
            for (TradeNpc row : source) {
                try { byId.put(UUID.fromString(row.getNpcId()), row); }
                catch (RuntimeException ignored) { }
            }
            npcs = new NpcIndex(key, source, Map.copyOf(byId), List.copyOf(byId.values()));
            indexBuilds++;
        }
        return npcs;
    }

    private static synchronized TradeIndex tradeIndex() {
        String path = OnServerStartedManager.PATH;
        List<Trade> source = path == null ? List.of() : new JsonRepository<>(Paths.get(path), "trades", Trade::fromJson, Trade::toJson).snapshot();
        String key = pathKey(path);
        if (trades == null || trades.source != source || !trades.path.equals(key)) {
            Map<String, Trade> byName = new LinkedHashMap<>();
            Map<String, List<Trade>> categories = new HashMap<>();
            for (Trade row : source) {
                if (row.getName() == null) continue;
                byName.putIfAbsent(row.getName(), row); // Preserve the previous findFirst lookup semantics.
                categories.computeIfAbsent(categoryKey(row.getCategory()), ignored -> new ArrayList<>()).add(row);
            }
            categories.replaceAll((category, rows) -> List.copyOf(rows));
            trades = new TradeIndex(key, source, Map.copyOf(byName), Map.copyOf(categories), List.copyOf(byName.values()));
            indexBuilds++;
        }
        return trades;
    }

    private static synchronized HistoryIndex historyIndex() {
        String path = OnServerStartedManager.PATH_HISTORY;
        List<TradeHistory> source = path == null ? List.of() : new JsonRepository<>(Paths.get(path), "history", TradeHistory::fromJson, TradeHistory::toJson).snapshot();
        String key = pathKey(path);
        if (histories == null || histories.source != source || !histories.path.equals(key)) {
            Map<String, TradeHistory> byId = new HashMap<>();
            Map<String, List<TradeHistory>> byNpc = new HashMap<>();
            for (TradeHistory row : source) {
                if (row.getId() != null) byId.putIfAbsent(row.getId(), row);
                if (row.getNpcId() != null && !row.isFinished() && !row.isAbandoned())
                    byNpc.computeIfAbsent(row.getNpcId(), ignored -> new ArrayList<>()).add(row);
            }
            byNpc.replaceAll((id, rows) -> List.copyOf(rows));
            histories = new HistoryIndex(key, source, Map.copyOf(byId), Map.copyOf(byNpc));
            indexBuilds++;
        }
        return histories;
    }

    private record NpcIndex(String path, List<TradeNpc> source, Map<UUID, TradeNpc> byId, List<TradeNpc> rows) { }
    private record TradeIndex(String path, List<Trade> source, Map<String, Trade> byName,
                              Map<String, List<Trade>> byCategory, List<Trade> rows) { }
    private record HistoryIndex(String path, List<TradeHistory> source, Map<String, TradeHistory> byId,
                                Map<String, List<TradeHistory>> byNpc) { }
}
