package fr.renblood.npcshopkeeper;

import fr.renblood.npcshopkeeper.client.renderer.TradeNpcRenderer;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModItems;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModTabs;
import fr.renblood.npcshopkeeper.manager.road.RoadTickScheduler;
import fr.renblood.npcshopkeeper.world.WorldEventHandler;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.init.EntityInit.ENTITY_TYPES;
import static fr.renblood.npcshopkeeper.init.EntityInit.TRADE_NPC_ENTITY;
import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.onServerStarted;

@Mod(Npcshopkeeper.MODID)
public class Npcshopkeeper {
    public static final String MODID = "npcshopkeeper";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    /** Changed to List<> to avoid immutable-to-ArrayList casting errors */
    public static List<CommercialRoad> COMMERCIAL_ROADS = new ArrayList<>();

    public Npcshopkeeper() {
        // Register to the Forge event bus
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(WorldEventHandler.class);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        ENTITY_TYPES.register(modBus);
        MENUS.register(modBus);
        NpcshopkeeperModItems.REGISTRY.register(modBus);
        NpcshopkeeperModMenus.REGISTRY.register(modBus);
        NpcshopkeeperModTabs.REGISTRY.register(modBus);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        onServerStarted(event);
        LOGGER.info("Event onServerStarting executed!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() ->
                EntityRenderers.register(
                        TRADE_NPC_ENTITY.get(),
                        TradeNpcRenderer::new
                )
        );
    }

    // Network channel setup
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(MODID, "network"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );
    private static int messageID = 0;

    public static <T> void addNetworkMessage(
            Class<T> messageType,
            BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, Supplier<NetworkEvent.Context>> consumer
    ) {
        PACKET_HANDLER.registerMessage(
                messageID++,
                messageType,
                encoder,
                decoder,
                consumer
        );
    }

    // Simple server-scheduled work queue
    private static final Collection<AbstractMap.SimpleEntry<Runnable,Integer>> workQueue =
            new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int ticks, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            workQueue.add(new AbstractMap.SimpleEntry<>(action, ticks));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
    }



}
