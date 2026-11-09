package fr.renblood.npcshopkeeper.manager.quest;

import fr.renblood.npcshopkeeper.manager.integration.MedievalCoinsIntegration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestNpcInteractionManager {
    private static final long INTERACTION_COOLDOWN_MS = 1500L;
    private static final Map<UUID, Long> LAST_INTERACTION = new ConcurrentHashMap<>();

    private static final java.util.concurrent.ThreadPoolExecutor WORKER=new java.util.concurrent.ThreadPoolExecutor(
        1,2,30,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<>(64),r->{
            Thread thread=new Thread(r,"NpcShopkeeper-QuestInteraction");thread.setDaemon(true);return thread;
        },new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    private QuestNpcInteractionManager() {
    }

    public static boolean handle(ServerPlayer player, List<String> questIds, String npcId, String npcName, String texture) {
        boolean hasQuestIds = questIds != null && !questIds.isEmpty();
        boolean hasNpcId = npcId != null && !npcId.isBlank();
        if (!hasQuestIds && !hasNpcId) return false;

        long now = System.currentTimeMillis();
        Long previous = LAST_INTERACTION.put(player.getUUID(), now);
        if (previous != null && now - previous < INTERACTION_COOLDOWN_MS) return true;

        if (hasNpcId && MedievalCoinsIntegration.openNpcQuestInteractions(player, npcId, npcName, texture)) return true;
        if (!hasQuestIds) return false;

        try{WORKER.execute(()->interact(player,questIds,npcName,texture));}
        catch(java.util.concurrent.RejectedExecutionException busy){player.sendSystemMessage(Component.literal("Les échanges de quêtes sont occupés. Réessayez dans un instant."));}
        return true;
    }

    private static void interact(ServerPlayer player, List<String> questIds, String npcName, String texture) {
        MedievalCoinsIntegration.getActiveQuest(player, questIds).ifPresentOrElse(
                state -> MedievalCoinsIntegration.getQuest(state.questId()).ifPresent(quest -> {
                    boolean completed = MedievalCoinsIntegration.completeQuest(player, state);
                    send(player, npcName, completed ? quest.endText() : "Cette quete ne peut pas encore etre rendue.",
                            texture, completed ? ChatFormatting.GREEN : ChatFormatting.RED);
                }),
                () -> MedievalCoinsIntegration.getQuest(questIds.get(0)).ifPresent(quest -> {
                    boolean started = MedievalCoinsIntegration.startQuest(player, quest.id());
                    send(player, npcName, started ? quest.beginText() : "Cette quete ne peut pas etre demarree.",
                            texture, started ? ChatFormatting.GOLD : ChatFormatting.RED);
                })
        );
    }

    private static void send(ServerPlayer player, String npcName, String text, String texture, ChatFormatting color) {
        String resolvedNpcName = npcName == null || npcName.isBlank() ? "ce PNJ" : npcName;
        String rawMessage = text == null || text.isBlank() ? "Quete mise a jour." : text;
        String message = rawMessage.replace("{npc}", resolvedNpcName).replace("{NPC}", resolvedNpcName);
        if (!MedievalCoinsIntegration.showNpcDialogue(player, npcName, message, texture)) {
            player.getServer().execute(() -> player.sendSystemMessage(Component.literal(message).withStyle(color)));
        }
    }
}
