package fr.renblood.npcshopkeeper.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.renblood.npcshopkeeper.data.api.NpcSpawn;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.data.NpcPlacementSavedData;
import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcSpawnManager;
import fr.renblood.npcshopkeeper.manager.npc.NpcEntityIndex;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.*;

public final class NpcPlacementCommands {
    private NpcPlacementCommands() {}
    private record Key(MinecraftServer server,String spawn) {}
    private static final Set<Key> PENDING=ConcurrentHashMap.newKeySet();
    private static final ThreadPoolExecutor WORKERS=new ThreadPoolExecutor(2,2,30,TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32),task->{Thread t=new Thread(task,"Renblood-NpcPlacement");t.setDaemon(true);t.setContextClassLoader(NpcPlacementCommands.class.getClassLoader());return t;});

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ns").then(Commands.literal("npc").requires(CommandPermissions::isAdmin).then(tp())));
    }
    private static LiteralArgumentBuilder<CommandSourceStack> tp() {
        return Commands.literal("tp").executes(c->move(c.getSource(),"nearest",c.getSource().getPosition()))
                .then(Commands.argument("spawnId",StringArgumentType.word()).suggests(NpcPlacementCommands::suggest)
                    .executes(c->move(c.getSource(),StringArgumentType.getString(c,"spawnId"),c.getSource().getPosition()))
                    .then(Commands.argument("position",Vec3Argument.vec3())
                        .executes(c->move(c.getSource(),StringArgumentType.getString(c,"spawnId"),Vec3Argument.getVec3(c,"position")))));
    }
    private static CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggest(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context,com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        var ids=new java.util.TreeSet<String>();ids.add("nearest");
        for(var npc:NpcEntityIndex.all())if(npc.level().getServer()==context.getSource().getServer()&&!npc.getApiNpcSpawnId().isBlank())ids.add(npc.getApiNpcSpawnId());
        for(var spawn:NpcPlacementSavedData.get(context.getSource().getServer()).snapshot())if(spawn.active)ids.add(spawn.spawn_id);
        return SharedSuggestionProvider.suggest(ids,builder);
    }
    public static String resolve(CommandSourceStack source,String target) {
        if("nearest".equalsIgnoreCase(target)) {
            var npc=NpcEntityIndex.all().stream().filter(n->n.level()==source.getLevel()&&!n.isRemoved()&&!n.getApiNpcSpawnId().isBlank()
                    &&n.distanceToSqr(source.getPosition())<=36)
                    .min(Comparator.comparingDouble(n->n.distanceToSqr(source.getPosition()))).orElse(null);
            if(npc==null)throw new IllegalArgumentException("Aucun PNJ décor/quête placé à moins de 6 blocs.");
            return npc.getApiNpcSpawnId();
        }
        // Accept an entity UUID too; it is different from the stable spawn ID.
        try {var npc=NpcEntityIndex.get(UUID.fromString(target));if(npc!=null&&!npc.getApiNpcSpawnId().isBlank())return npc.getApiNpcSpawnId();}
        catch(IllegalArgumentException ignored) {}
        if(target==null||target.isBlank()||target.length()>255)throw new IllegalArgumentException("Identifiant de spawn invalide.");
        return target;
    }
    public static int delete(CommandSourceStack source,String target) {
        JsonObject body=new JsonObject();body.addProperty("action","deactivate");return edit(source,target,body);
    }
    private static int move(CommandSourceStack source,String target,Vec3 at) {
        if(!Double.isFinite(at.x)||!Double.isFinite(at.y)||!Double.isFinite(at.z)
                ||at.y<source.getLevel().getMinBuildHeight()||at.y>=source.getLevel().getMaxBuildHeight()
                ||!source.getLevel().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(at))) {
            source.sendFailure(Component.literal("Destination en dehors des limites de cette dimension."));return 0;
        }
        JsonObject body=new JsonObject();body.addProperty("action","move");body.addProperty("world",source.getLevel().dimension().location().toString());
        body.addProperty("x",at.x);body.addProperty("y",at.y);body.addProperty("z",at.z);
        body.addProperty("yaw",source.getRotation().y);body.addProperty("pitch",source.getRotation().x);
        return edit(source,target,body);
    }
    private static int edit(CommandSourceStack source,String target,JsonObject body) {
        String id;
        try{id=resolve(source,target);}catch(IllegalArgumentException error){source.sendFailure(Component.literal(error.getMessage()));return 0;}
        MinecraftServer server=source.getServer();Key key=new Key(server,id);
        if(!PENDING.add(key)){source.sendFailure(Component.literal("Une modification de ce PNJ est déjà en cours."));return 0;}
        String actor=source.getEntity()==null?null:source.getEntity().getStringUUID();
        source.sendSuccess(()->Component.literal("Enregistrement du placement PNJ…").withStyle(ChatFormatting.YELLOW),false);
        try {
            CompletableFuture.supplyAsync(()->MedievalCoinsIntegration.editNpcSpawn(actor,id,body),WORKERS)
                    .whenComplete((spawn,error)->{
                        PENDING.remove(key);
                        server.execute(()->{
                            if(error!=null){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();source.sendFailure(Component.literal("PNJ non modifié : "+cause.getMessage()));return;}
                            GlobalNpcSpawnManager.applyAdminEdit(server,spawn);
                            boolean deleted="deactivate".equals(body.get("action").getAsString());
                            String message=deleted?"Apparition du PNJ supprimée durablement. Sa fiche et ses quêtes sont conservées."
                                    :"PNJ déplacé : "+spawn.x+", "+spawn.y+", "+spawn.z+" ("+spawn.world+").";
                            if(!deleted&&!server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,new net.minecraft.resources.ResourceLocation(spawn.world))).hasChunkAt(spawn.getPos()))message+=" Il apparaîtra au chargement de ce chunk.";
                            String result=message;source.sendSuccess(()->Component.literal(result).withStyle(ChatFormatting.GREEN),true);
                        });
                    });
        }catch(RejectedExecutionException busy){PENDING.remove(key);source.sendFailure(Component.literal("File de modifications occupée. Réessaie dans un instant."));return 0;}
        return 1;
    }
    public static int info(CommandSourceStack source,String target) {
        String id;
        try{id=resolve(source,target);}catch(IllegalArgumentException error){source.sendFailure(Component.literal(error.getMessage()));return 0;}
        TradeNpcEntity npc=NpcEntityIndex.getBySpawnId(id);
        if(npc==null){source.sendFailure(Component.literal("PNJ non chargé. Tu peux utiliser son spawn ID avec /ns npc tp "+id));return 0;}
        source.sendSuccess(()->Component.literal(npc.getNpcName()+" · "+npc.getPersistentData().getString("ApiNpcType")
                +"\nSpawn ID : "+id+"\nEntité : "+npc.getUUID()+"\nFiche : "+npc.getPersistentData().getString("ApiNpcId")
                +"\nPosition : "+npc.getX()+", "+npc.getY()+", "+npc.getZ()).withStyle(ChatFormatting.GOLD),false);
        source.sendSuccess(()->Component.literal("[Amener ici] ").withStyle(style->style.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/ns npc tp "+id)))
                .append(Component.literal("[Supprimer l’apparition]").withStyle(style->style.withColor(ChatFormatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/ns npc delete "+id)))),false);
        return 1;
    }
}
