package fr.renblood.npcshopkeeper.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.manager.data.DataValidator;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsEventBridge;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcSpawnManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import fr.renblood.npcshopkeeper.manager.npc.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.manager.road.RoadTickScheduler;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber
public final class NsCommand {
    private NsCommand() {
    }

    private static final SuggestionProvider<CommandSourceStack> SPAWN_IDS = (context, builder) ->
            SharedSuggestionProvider.suggest(NpcEntityIndex.all().stream()
                    .filter(npc -> npc.level() == context.getSource().getLevel())
                    .map(TradeNpcEntity::getApiNpcSpawnId)
                    .filter(id -> id != null && !id.isBlank()).distinct(), builder);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("ns")
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(Commands.literal("status")
                        .requires(CommandPermissions::isAdmin)
                        .executes(context -> showStatus(context.getSource())))
                .then(Commands.literal("shop").requires(CommandPermissions::isAdmin)
                    .then(Commands.literal("create").executes(c->{var p=c.getSource().getPlayerOrException();fr.renblood.npcshopkeeper.services.WorkshopServer.create(p,p.blockPosition());return 1;})))
                .then(Commands.literal("npc")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("create")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    CreateNpcCommand.openGui(player);
                                    return 1;
                                }))
                        .then(Commands.literal("list")
                                .executes(context -> listNpcs(context.getSource())))
                        .then(Commands.literal("spawn")
                                .then(Commands.literal("decor")
                                        .then(Commands.argument("name", StringArgumentType.string())
                                                .suggests(SpawnDecorNpcCommand.NPC_NAME_SUGGESTIONS)
                                                .executes(context -> SpawnDecorNpcCommand.execute(context.getSource(),
                                                        StringArgumentType.getString(context, "name")))))
                                .then(Commands.literal("trade")
                                        .requires(CommandPermissions::isAdmin)
                                        .executes(context -> SpawnTradeNpcCommand.executeCommand(context.getSource()))))
                        .then(Commands.literal("merchant")
                                .requires(CommandPermissions::isAdmin)
                                .executes(context -> SpawnTradeNpcCommand.executeCommand(context.getSource())))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("spawnId", StringArgumentType.word()).suggests(SPAWN_IDS)
                                        .executes(context -> deleteNpcSpawn(context.getSource(),
                                                StringArgumentType.getString(context, "spawnId"))))
                                .then(Commands.literal("nearest")
                                        .executes(context -> deleteNearestNpcSpawn(context.getSource())))))
                .then(Commands.literal("trade")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("create")
                                .executes(context -> CreateTradeCommand.execute(context.getSource())))
                        .then(Commands.literal("open")
                                .then(Commands.argument("name", StringArgumentType.string()).suggests(TradeCommand.TRADE_SUGGESTIONS)
                                        .executes(context -> TradeCommand.execute(context.getSource(),
                                                StringArgumentType.getString(context, "name"))))))
                .then(Commands.literal("road")
                        .then(Commands.literal("economy").requires(CommandPermissions::isAdmin).executes(c->{fr.renblood.npcshopkeeper.services.WorkshopServer.openRoutes(c.getSource().getPlayerOrException());return 1;}))
                        .then(Commands.literal("define")
                                .requires(source -> source.getEntity() instanceof ServerPlayer)
                                .then(Commands.literal("remove")
                                        .executes(context -> PointDefiningCommand.execute(context.getSource(), "remove")))
                                .then(Commands.literal("cancel")
                                        .executes(context -> PointDefiningCommand.execute(context.getSource(), "cancel")))
                                .then(Commands.literal("finish")
                                        .executes(context -> PointDefiningCommand.execute(context.getSource(), "finish"))))
                        .then(Commands.literal("list")
                                .requires(CommandPermissions::isAdmin)
                                .executes(context -> SeeRoadsCommand.execute(context.getSource())))
                        .then(Commands.literal("status")
                                .requires(CommandPermissions::isAdmin)
                                .then(Commands.argument("route", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String input = builder.getRemainingLowerCase();
                                            String prefix = !input.isEmpty() && com.mojang.brigadier.StringReader.isQuotedStringStart(input.charAt(0))
                                                    ? input.substring(1) : input;
                                            Npcshopkeeper.COMMERCIAL_ROADS.stream().map(CommercialRoad::getName)
                                                    .filter(name -> name.toLowerCase(java.util.Locale.ROOT).startsWith(prefix))
                                                    .distinct().limit(50).map(StringArgumentType::escapeIfRequired)
                                                    .forEach(builder::suggest);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> roadStatus(context.getSource(), StringArgumentType.getString(context, "route")))))
                        .then(Commands.literal("refresh")
                                .requires(CommandPermissions::isAdmin)
                                .executes(context -> reloadRoutes(context.getSource()))))
                .then(Commands.literal("harbor")
                        .then(Commands.literal("edit").requires(CommandPermissions::isAdmin).then(Commands.argument("port",StringArgumentType.string()).suggests((c,b)->SharedSuggestionProvider.suggest(PortManager.getAllPorts().stream().map(port->StringArgumentType.escapeIfRequired(port.getName())),b)).executes(c->{fr.renblood.npcshopkeeper.services.VoyageService.open(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"port"),true);return 1;})))
                        .then(Commands.literal("ticket").executes(c->{fr.renblood.npcshopkeeper.services.VoyageService.showTicket(c.getSource().getPlayerOrException());return 1;}))
                        .then(Commands.literal("create").requires(CommandPermissions::isAdmin)
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(context -> SetHarborCommand.execute(context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("config").requires(CommandPermissions::isAdmin)
                                .then(Commands.literal("price")
                                        .then(Commands.argument("blocks_per_iron", IntegerArgumentType.integer(1))
                                                .executes(context -> SetTravelPriceCommand.setPrice(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "blocks_per_iron")))))
                                .then(Commands.literal("day_length")
                                        .then(Commands.argument("minutes", IntegerArgumentType.integer(1))
                                                .executes(context -> SetTravelPriceCommand.setDayLength(context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "minutes")))))))
                .then(Commands.literal("reference")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("missing")
                                .executes(context -> showMissingReferences(context.getSource())))
                        .then(Commands.literal("price")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests(CreateReferenceCommand.ITEM_SUGGESTIONS)
                                        .then(Commands.argument("min", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("max", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("sendToBackend", BoolArgumentType.bool())
                                                        .executes(context -> CreateReferenceCommand.execute(context.getSource(),
                                                                ResourceLocationArgument.getId(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "min"),
                                                                IntegerArgumentType.getInteger(context, "max"),
                                                                BoolArgumentType.getBool(context, "sendToBackend"))))))))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("item", ResourceLocationArgument.id())
                                        .suggests(CreateReferenceCommand.ITEM_SUGGESTIONS)
                                        .then(Commands.argument("job", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(SetXpReferenceCommand.JOBS, builder))
                                                .then(Commands.argument("min", FloatArgumentType.floatArg(0))
                                                        .then(Commands.argument("max", FloatArgumentType.floatArg(0))
                                                                .executes(context -> SetXpReferenceCommand.execute(context.getSource(),
                                                                        ResourceLocationArgument.getId(context, "item").toString(),
                                                                        StringArgumentType.getString(context, "job"),
                                                                        FloatArgumentType.getFloat(context, "min"),
                                                                        FloatArgumentType.getFloat(context, "max")))))))))
                .then(Commands.literal("debug")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("performance").executes(context -> debugPerformance(context.getSource())))
                        .then(Commands.literal("npcs").executes(context -> debugNpcs(context.getSource())))
                        .then(Commands.literal("sync").executes(context -> debugSync(context.getSource())))
                        .then(Commands.literal("legacy")
                                .then(Commands.literal("log")
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> DebugCommand.setLog(context.getSource(),
                                                        BoolArgumentType.getBool(context, "enabled"))))
                                        .executes(context -> DebugCommand.toggleLog(context.getSource())))
                                .then(Commands.literal("add_dummy_roads")
                                        .executes(context -> DebugCommand.addDummyRoads(context.getSource())))))
                .then(Commands.literal("reload")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("trades").executes(context -> reloadTrades(context.getSource())))
                        .then(Commands.literal("npcs").executes(context -> reloadNpcs(context.getSource())))
                        .then(Commands.literal("decors").executes(context -> reloadDecorNpcs(context.getSource())))
                        .then(Commands.literal("routes").executes(context -> reloadRoutes(context.getSource())))
                        .then(Commands.literal("all").executes(context -> reloadAll(context.getSource()))))
                .then(Commands.literal("sync")
                        .requires(CommandPermissions::isAdmin)
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("spawnId", StringArgumentType.word()).suggests(SPAWN_IDS)
                                        .executes(context -> syncSpawn(context.getSource(),
                                                StringArgumentType.getString(context, "spawnId")))))));

        dispatcher.register(Commands.literal("npcshopkeeper")
                .redirect(dispatcher.getRoot().getChild("ns")));
    }

    private static int showHelp(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NpcShopkeeper - commands /npcshopkeeper")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        helpLine(source, "/ns help", "Aide NPC Shopkeeper.");
        source.sendSuccess(() -> Component.literal("Client d’auberge : clic droit pour voir la commande, puis Maj + clic droit avec le plat en main."), false);
        helpLine(source, "/ns road define <remove|cancel|finish>", "Gère la définition d'une route.");

        if (CommandPermissions.isAdmin(source)) {
            helpLine(source, "/ns status", "Show mod status.");
            helpLine(source, "/ns shop create", "Place une boutique permanente ; Maj + clic droit pour la modifier.");
            helpLine(source, "/ns road economy", "Associe une ville aux prix des contrats de route.");
            helpLine(source, "/ns npc create", "Open NPC creation.");
            helpLine(source, "/ns npc spawn decor <name>", "Place a decorative NPC.");
            helpLine(source, "/ns npc spawn merchant", "Place a merchant NPC.");
            helpLine(source, "/ns trade open <name>", "Open an existing trade.");
            helpLine(source, "/ns road list", "Ouvre les routes commerciales.");
            helpLine(source, "/ns road status <route>", "Affiche le prochain essai et le dernier résultat.");
            helpLine(source, "/ns harbor edit <port>", "Configure carte, horaires, destinations et tarifs au quai, même hors service.");
            helpLine(source, "/ns harbor create <name>", "Create a harbor and captain.");
            helpLine(source, "/ns harbor config <price|day_length> <value>", "Configure travel.");
            helpLine(source, "/ns reference price <item> <min> <max> <sendToBackend>", "Configure une reference locale et optionnellement backend.");
            helpLine(source, "/ns reference xp <item> <job> <min> <max>", "Configure XP references.");
            helpLine(source, "/ns debug <performance|npcs|sync>", "Show server metrics.");
            helpLine(source, "/ns reload <trades|npcs>", "Reload a target cache.");
            helpLine(source, "/ns sync spawn <spawnId>", "Synchronize one MedievalCoins spawn.");
        }

        if (CommandPermissions.isAdmin(source)) {
            helpLine(source, "/ns trade create", "Open trade creation.");
        }
        return 1;
    }

    private static void helpLine(CommandSourceStack source, String command, String description) {
        MutableComponent line = Component.literal(command)
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" - " + description).withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> line, false);
    }

    private static int showStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NpcShopkeeper - état")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        long tradeRoutes = Npcshopkeeper.COMMERCIAL_ROADS.stream().filter(CommercialRoad::isTradeRoute).count();
        long menuRoutes = Npcshopkeeper.COMMERCIAL_ROADS.stream().filter(CommercialRoad::isInnMenuRoute).count();
        source.sendSuccess(() -> Component.literal("Routes: " + tradeRoutes + " commerce | " + menuRoutes + " menu auberge"), false);
        source.sendSuccess(() -> Component.literal("Ports: " + PortManager.getAllPorts().size()), false);
        source.sendSuccess(() -> Component.literal("Debug: " + (Npcshopkeeper.DEBUG_MODE ? "activé" : "désactivé")), false);
        return 1;
    }

    private static int showMissingReferences(CommandSourceStack source) {
        var missing = fr.renblood.npcshopkeeper.api.NpcShopkeeperAPI.getMissingTradeReferenceItemIds();
        source.sendSuccess(() -> Component.literal("References de trade manquantes: " + missing.size())
                .withStyle(missing.isEmpty() ? ChatFormatting.GREEN : ChatFormatting.GOLD), false);
        if (!missing.isEmpty()) {
            missing.stream().limit(20).forEach(item -> source.sendSuccess(() -> Component.literal(item)
                    .withStyle(style -> style.withColor(ChatFormatting.AQUA)
                            .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.SUGGEST_COMMAND,
                                    "/ns reference price " + item + " "))
                            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Préparer la référence : complète les prix minimum, maximum et l’envoi au backend.")))), false));
            if (missing.size() > 20) {
                source.sendSuccess(() -> Component.literal("... et " + (missing.size() - 20) + " autres."), false);
            }
        }
        return missing.size();
    }

    private static int roadStatus(CommandSourceStack source, String selector) {
        for (CommercialRoad road : Npcshopkeeper.COMMERCIAL_ROADS) {
            if (road.getId().equalsIgnoreCase(selector) || road.getName().equalsIgnoreCase(selector)) {
                long loaded = road.getPositions().stream().filter(pos -> source.getServer().overworld().getChunkSource()
                        .hasChunk(pos.getX() >> 4, pos.getZ() >> 4)).count();
                source.sendSuccess(() -> Component.literal(RoadTickScheduler.status(road, source.getServer().getTickCount())
                        + " · Points chargés : " + loaded + "/" + road.getPositions().size()), false);
                return 1;
            }
        }
        source.sendFailure(Component.literal("Route introuvable."));
        return 0;
    }

    private static int debugPerformance(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("JSON lectures/ecritures: " + JsonFileManager.getReadCount() + "/" + JsonFileManager.getWriteCount()), false);
        source.sendSuccess(() -> Component.literal("Cache repositories: " + JsonRepository.getCachedRepositoryCount()
                + " | hits/misses: " + JsonRepository.getCacheHits() + "/" + JsonRepository.getCacheMisses()), false);
        source.sendSuccess(() -> Component.literal("Routes planifiees: " + RoadTickScheduler.scheduledCount()
                + " | prochaine echeance: " + RoadTickScheduler.getNextDueTick()), false);
        return 1;
    }

    private static int debugNpcs(CommandSourceStack source) {
        var issues = DataValidator.validate();
        source.sendSuccess(() -> Component.literal("PNJ JSON/index/API: " + RuntimeDataCache.npcCount() + "/"
                + NpcEntityIndex.size() + "/" + NpcEntityIndex.apiSize()), false);
        source.sendSuccess(() -> Component.literal("Trades caches: " + RuntimeDataCache.tradeCount() + " · Reconstructions d’index : " + RuntimeDataCache.indexBuildCount()
                + " | problemes detectes: " + issues.size()), false);
        issues.stream().limit(10).forEach(issue -> source.sendFailure(Component.literal(issue)));
        return 1;
    }

    private static int debugSync(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Medieval Coins: " + (MedievalCoinsIntegration.isLoaded() ? "actif" : "absent")
                + " | sync en cours: " + GlobalNpcSpawnManager.isSynchronizationRunning()), false);
        source.sendSuccess(() -> Component.literal("Derniere sync: " + GlobalNpcSpawnManager.getLastSyncAt()
                + " | duree: " + GlobalNpcSpawnManager.getLastSyncDurationMs() + " ms"
                + " | +" + GlobalNpcSpawnManager.getLastSpawned() + " ~" + GlobalNpcSpawnManager.getLastUpdated()
                + " -" + GlobalNpcSpawnManager.getLastRemoved()), false);
        source.sendSuccess(() -> Component.literal("Evenements Medieval Coins: " + MedievalCoinsEventBridge.getEventCount()
                + " | dernier: " + MedievalCoinsEventBridge.getLastEventAt()), false);
        return 1;
    }

    private static int reloadTrades(CommandSourceStack source) {
        RuntimeDataCache.reloadTrades();
        source.sendSuccess(() -> Component.literal("Trades recharges: " + RuntimeDataCache.tradeCount()), true);
        return 1;
    }

    private static int reloadNpcs(CommandSourceStack source) {
        GlobalNpcManager.loadNpcData();
        RuntimeDataCache.reloadNpcs();
        source.sendSuccess(() -> Component.literal("PNJ recharges: " + RuntimeDataCache.npcCount()), true);
        return 1;
    }

    private static int reloadDecorNpcs(CommandSourceStack source) {
        GlobalNpcManager.loadNpcData();
        MedievalCoinsIntegration.getNpcs().forEach(GlobalNpcManager::addNpcFromTemplate);
        GlobalNpcSpawnManager.synchronizeAsync(source.getServer());
        source.sendSuccess(() -> Component.literal("Catalogue PNJ decor recharge: "
                + GlobalNpcManager.getAllNpcData().size()
                + " PNJ disponibles. Sync des spawns lancee."), true);
        return 1;
    }

    private static int listNpcs(CommandSourceStack source) {
        if (GlobalNpcManager.getAllNpcData().isEmpty()) {
            reloadDecorNpcs(source);
        }

        String names = GlobalNpcManager.getAllNpcData().keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(name -> name.replace(' ', '_'))
                .collect(Collectors.joining(", "));

        if (names.isBlank()) {
            source.sendFailure(Component.literal("Aucun PNJ disponible. Essayez /ns reload decors."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("PNJ disponibles (" + GlobalNpcManager.getAllNpcData().size() + "):"), false);
        source.sendSuccess(() -> Component.literal(names), false);
        return 1;
    }

    private static int reloadRoutes(CommandSourceStack source) {
        ServerLevel level = source.getServer().overworld();
        JsonRepository.invalidate(Paths.get(OnServerStartedManager.PATH_COMMERCIAL));
        JsonRepository<CommercialRoad> roadRepo = new JsonRepository<>(
                Paths.get(OnServerStartedManager.PATH_COMMERCIAL),
                "roads",
                json -> CommercialRoad.fromJson(json, level),
                CommercialRoad::toJson
        );
        Npcshopkeeper.COMMERCIAL_ROADS = roadRepo.loadAll();
        NpcSpawnerManager.activeNPCs.clear();
        RoadTickScheduler.clear();
        Npcshopkeeper.COMMERCIAL_ROADS.forEach(RoadTickScheduler::registerRoad);
        source.sendSuccess(() -> Component.literal("Routes rechargees: " + Npcshopkeeper.COMMERCIAL_ROADS.size()), true);
        return 1;
    }

    private static int reloadAll(CommandSourceStack source) {
        reloadTrades(source);
        reloadNpcs(source);
        reloadRoutes(source);
        GlobalNpcSpawnManager.synchronizeAsync(source.getServer());
        source.sendSuccess(() -> Component.literal("Reload global demande, sync PNJ API lancee."), true);
        return 1;
    }

    private static int syncSpawn(CommandSourceStack source, String spawnId) {
        GlobalNpcSpawnManager.synchronizeSpawnAsync(source.getServer(), spawnId);
        source.sendSuccess(() -> Component.literal("Synchronisation demandee pour le spawn " + spawnId), true);
        return 1;
    }

    private static int deleteNpcSpawn(CommandSourceStack source, String spawnId) {
        GlobalNpcSpawnManager.DeleteResult result = GlobalNpcSpawnManager.deleteSpawn(source.getServer(), spawnId);
        source.sendSuccess(() -> Component.literal("Spawn PNJ supprime: " + spawnId
                + " | local=" + result.localRemoved()
                + " | api=" + (result.apiUpdated() ? "ok" : "non confirme")), true);
        return result.localRemoved() > 0 || result.apiUpdated() ? 1 : 0;
    }

    private static int deleteNearestNpcSpawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        AABB area = player.getBoundingBox().inflate(6.0D);
        TradeNpcEntity nearest = player.serverLevel().getEntitiesOfClass(TradeNpcEntity.class, area,
                        entity -> !entity.getPersistentData().getString("ApiNpcSpawnId").isBlank())
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)))
                .orElse(null);
        if (nearest == null) {
            source.sendFailure(Component.literal("Aucun PNJ API trouve a moins de 6 blocs."));
            return 0;
        }
        return deleteNpcSpawn(source, nearest.getPersistentData().getString("ApiNpcSpawnId"));
    }
}
