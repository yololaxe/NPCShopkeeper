package fr.renblood.npcshopkeeper.command;
import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.npc.*;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;
@Mod.EventBusSubscriber(modid="npcshopkeeper")
public final class RoadNpcAdmin {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("ns").then(Commands.literal("npc").requires(CommandPermissions::isAdmin)
        .then(Commands.literal("info").executes(c->info(c.getSource(),"nearest")).then(Commands.argument("uuid",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(NpcEntityIndex.all().stream().map(n->n.getStringUUID()),b)).executes(c->info(c.getSource(),StringArgumentType.getString(c,"uuid")))))
        .then(Commands.literal("remove").then(Commands.argument("uuid",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(NpcEntityIndex.all().stream().map(n->n.getStringUUID()),b)).executes(c->remove(c.getSource(),StringArgumentType.getString(c,"uuid")))))));}
    private static TradeNpcEntity find(CommandSourceStack source,String selector){if(selector.equals("nearest"))return NpcEntityIndex.all().stream().filter(n->n.level()==source.getLevel()&&!n.isRemoved()&&n.position().distanceToSqr(source.getPosition())<=144).min(Comparator.comparingDouble(n->n.position().distanceToSqr(source.getPosition()))).orElse(null);try{return NpcEntityIndex.get(UUID.fromString(selector));}catch(IllegalArgumentException invalid){return null;}}
    private static void line(CommandSourceStack source,String text){source.sendSuccess(()->Component.literal(text),false);}
    private static int info(CommandSourceStack source,String selector){var n=find(source,selector);if(n==null){source.sendFailure(Component.literal("Aucun PNJ chargé trouvé (rayon de 12 blocs pour nearest)."));return 0;}var model=n.getTradeNpc();var roads=Npcshopkeeper.COMMERCIAL_ROADS.stream().filter(r->r.getNpcEntities().stream().anyMatch(e->e.getUUID().equals(n.getUUID()))).toList();
        line(source,"PNJ : "+n.getNpcName()+" · UUID : "+n.getStringUUID());line(source,"Monde : "+n.level().dimension().location()+" · Position : "+n.blockPosition().toShortString());
        line(source,"Type : "+(!n.getPersistentData().getString("ApiNpcSpawnId").isBlank()?"API":"local")+" · Modèle : "+(model==null?"absent":model.getNpcId())+" · PNJ de route : "+(model!=null&&model.isRouteNpc()));
        line(source,"Routes : "+(roads.isEmpty()?"aucune liaison persistante (orphelin possible)":roads.stream().map(r->r.getName()+" ["+r.getId()+"]").collect(java.util.stream.Collectors.joining(", "))));
        line(source,"Catégorie : "+(model==null?"inconnue":model.getTradeCategory())+" · Ville : "+n.getPersistentData().getString("RenbloodCityId"));line(source,"Skin : "+n.getTexture()+" · Spawn API : "+n.getPersistentData().getString("ApiNpcSpawnId"));
        source.sendSuccess(()->Component.literal("[Préparer la suppression de ce PNJ local]").withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/ns npc remove "+n.getStringUUID()))),false);return 1;}
    private static int remove(CommandSourceStack source,String id){var n=find(source,id);if(n==null){source.sendFailure(Component.literal("PNJ non chargé ou UUID invalide. Utilise /ns npc info près de lui."));return 0;}
        if(!n.getPersistentData().getString("ApiNpcSpawnId").isBlank()||n.isCaptain()||n.isInnCustomer()||n.getPersistentData().contains("RenbloodShopId")){source.sendFailure(Component.literal("PNJ géré par un autre service : utilise son outil de gestion. Pour un spawn API : /ns npc delete <spawnId>."));return 0;}
        try{
            Path npcFile=Paths.get(OnServerStartedManager.PATH_NPCS),roadFile=Paths.get(OnServerStartedManager.PATH_COMMERCIAL);Path backup=npcFile.getParent().resolve("admin-backups").resolve(System.currentTimeMillis()+"-"+n.getStringUUID());Files.createDirectories(backup);for(Path p:List.of(npcFile,roadFile))if(Files.exists(p))Files.copy(p,backup.resolve(p.getFileName()));
            var repo=new JsonRepository<TradeNpc>(npcFile,"npcs",TradeNpc::fromJson,TradeNpc::toJson);var entries=repo.loadAll();entries.removeIf(e->n.getStringUUID().equals(e.getNpcId()));repo.saveAll(entries);
            var deleted=Deleted.get(source.getServer());deleted.ids.add(n.getUUID());deleted.setDirty();source.getServer().overworld().getDataStorage().save();
            for(var road:Npcshopkeeper.COMMERCIAL_ROADS)road.getNpcEntities().removeIf(e->e.getUUID().equals(n.getUUID()));
            new JsonRepository<CommercialRoad>(roadFile,"roads",j->CommercialRoad.fromJson(j,source.getLevel()),CommercialRoad::toJson).saveAll(Npcshopkeeper.COMMERCIAL_ROADS);
            for(var map:NpcSpawnerManager.activeNPCs.values())map.values().removeIf(e->e.getUUID().equals(n.getUUID()));
            GlobalNpcManager.deactivateNpc(n.getNpcName());n.discard();line(source,"PNJ supprimé du monde et des sauvegardes locales. Sauvegarde : "+backup.getFileName());line(source,"Une route toujours active peut générer un autre PNJ ; cette ancienne identité ne sera plus restaurée.");return 1;
        }catch(Exception failure){source.sendFailure(Component.literal("Suppression non entièrement confirmée : vérifie les sauvegardes admin-backups avant de réessayer."));Npcshopkeeper.LOGGER.error("NPC removal failed for {}",id,failure);return 0;}}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide()&&event.getEntity() instanceof TradeNpcEntity npc&&Deleted.get(event.getLevel().getServer()).ids.contains(npc.getUUID()))event.setCanceled(true);}
    public static final class Deleted extends SavedData {
        final Set<UUID> ids=new HashSet<>();
        static Deleted get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(Deleted::load,Deleted::new,"renblood_removed_npcs");}
        public boolean contains(UUID id){return ids.contains(id);}
        public static Deleted load(CompoundTag tag){Deleted d=new Deleted();for(var t:tag.getList("ids",8))try{d.ids.add(UUID.fromString(t.getAsString()));}catch(IllegalArgumentException invalid){}return d;}
        @Override public CompoundTag save(CompoundTag tag){var list=new net.minecraft.nbt.ListTag();for(var id:ids)list.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));tag.put("ids",list);return tag;}
    }
}
