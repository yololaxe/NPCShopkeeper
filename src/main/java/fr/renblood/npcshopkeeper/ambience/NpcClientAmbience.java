package fr.renblood.npcshopkeeper.ambience;

import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Local, positional town details. One shared sound budget, even in crowded towns. */
@Mod.EventBusSubscriber(modid="npcshopkeeper",value=Dist.CLIENT)
public final class NpcClientAmbience {
    private static final ResourceLocation BANKER=new ResourceLocation("medieval_coins","banker");
    private static final RandomSource RANDOM=RandomSource.create();
    private static Object lastLevel;
    private static long nextAmbient,nextGreeting;
    private static int lastNpc=-1;
    private NpcClientAmbience(){}
    private static boolean custom(Mob mob){return mob instanceof TradeNpcEntity||BANKER.equals(ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()));}
    private static boolean audible(Mob mob,NpcAmbientSettings settings) {
        var player=Minecraft.getInstance().player;
        return player!=null&&custom(mob)&&NpcAmbientSettings.audible(mob.isAlive(),mob.isSilent(),mob.isInvisible(),mob.isSleeping(),mob.distanceToSqr(player),settings.radius);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null){lastLevel=null;return;}
        long now=mc.level.getGameTime();if(lastLevel!=mc.level){lastLevel=mc.level;nextAmbient=now+40+RANDOM.nextInt(60);nextGreeting=0;lastNpc=-1;}
        if(mc.isPaused()||now<nextAmbient)return;
        NpcAmbientSettings settings=NpcAmbientSettings.get();
        if(!settings.enabled||settings.volume<=0||mc.screen!=null||mc.options.getSoundSourceVolume(SoundSource.AMBIENT)<=0){nextAmbient=now+100;return;}
        nextAmbient=now+settings.minDelaySeconds*20L+RANDOM.nextInt((settings.maxDelaySeconds-settings.minDelaySeconds)*20+1);
        var nearby=new ArrayList<Mob>();
        for(var entity:mc.level.entitiesForRendering())if(entity instanceof Mob mob&&audible(mob,settings)) {
            nearby.add(mob);nearby.sort(Comparator.comparingDouble(npc->npc.distanceToSqr(mc.player)));if(nearby.size()>4)nearby.remove(nearby.size()-1);
        }
        Collections.shuffle(nearby,new Random(RANDOM.nextLong()));
        for(var npc:nearby)if(mc.player.hasLineOfSight(npc)) {
            if(npc.getId()==lastNpc&&nearby.size()>1)continue;
            boolean captain=npc instanceof TradeNpcEntity trade&&trade.isCaptain();boolean banker=BANKER.equals(ForgeRegistries.ENTITY_TYPES.getKey(npc.getType()));
            int variant=RANDOM.nextInt(5);SoundEvent sound=variant<3?SoundEvents.VILLAGER_AMBIENT:variant==3?SoundEvents.ARMOR_EQUIP_LEATHER:banker||!captain?SoundEvents.BOOK_PAGE_TURN:SoundEvents.ARMOR_EQUIP_LEATHER;
            float pitch=.94f+(Math.floorMod(npc.getUUID().hashCode(),8))*.02f;
            mc.getSoundManager().play(new SimpleSoundInstance(sound,SoundSource.AMBIENT,settings.volume*(variant<3?.65f:.45f),pitch,RANDOM,npc.getX(),npc.getEyeY(),npc.getZ()));lastNpc=npc.getId();break;
        }
    }
    @SubscribeEvent public static void greet(PlayerInteractEvent.EntityInteract event) {
        if(!event.getLevel().isClientSide||event.getHand()!=InteractionHand.MAIN_HAND||!(event.getTarget() instanceof TradeNpcEntity npc)||event.getEntity().isShiftKeyDown())return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player!=event.getEntity())return;var settings=NpcAmbientSettings.get();long now=mc.level.getGameTime();
        if(!settings.enabled||!settings.greetings||settings.volume<=0||now<nextGreeting||!audible(npc,settings))return;
        nextGreeting=now+80;nextAmbient=Math.max(nextAmbient,now+160);
        mc.getSoundManager().play(new SimpleSoundInstance(SoundEvents.VILLAGER_YES,SoundSource.AMBIENT,settings.volume*.8f,1,RANDOM,npc.getX(),npc.getEyeY(),npc.getZ()));
    }
}
