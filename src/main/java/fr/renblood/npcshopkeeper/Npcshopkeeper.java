package fr.renblood.npcshopkeeper;

import fr.renblood.npcshopkeeper.client.renderer.TradeNpcRenderer;
import fr.renblood.npcshopkeeper.data.commercial.CommercialRoad;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModItems;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModTabs;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.manager.NpcSpawnerManager;
import fr.renblood.npcshopkeeper.world.WorldEventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static fr.renblood.npcshopkeeper.init.EntityInit.ENTITY_TYPES;
import static fr.renblood.npcshopkeeper.init.EntityInit.TRADE_NPC_ENTITY;
import static fr.renblood.npcshopkeeper.manager.OnServerStartedManager.onServerStarted;

// The value here should match an entry in the META-INF/mods.toml file

@Mod("npcshopkeeper")
public class Npcshopkeeper {
    public static final Logger LOGGER = LogManager.getLogger(Npcshopkeeper.class);

    public static final String MODID = "npcshopkeeper";
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static ArrayList<CommercialRoad> COMMERCIAL_ROADS = new ArrayList<>();

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        onServerStarted(event);
        System.out.println("Evénement onServerStarting exécuté !");
        LOGGER.info("Evénement onServerStarting exécuté !");
    }

    public Npcshopkeeper() {
        // Start of user code block mod constructor
        // End of user code block mod constructor

        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(WorldEventHandler.class);
        MinecraftForge.EVENT_BUS.register(JsonTradeFileManager.class);
        ENTITY_TYPES.register(bus);
        MENUS.register(bus);
        NpcshopkeeperModItems.REGISTRY.register(bus);
        NpcshopkeeperModMenus.REGISTRY.register(bus);
        NpcshopkeeperModTabs.REGISTRY.register(bus);
        MinecraftForge.EVENT_BUS.register(Npcshopkeeper.class);


        // Start of user code block mod init
        // End of user code block mod init

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(TRADE_NPC_ENTITY.get(), TradeNpcRenderer::new);
        });
    }


    // Start of user code block mod methods
    // End of user code block mod methods
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public static <T> void addNetworkMessage(Class<T> messageType, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, Supplier<NetworkEvent.Context>> messageConsumer) {
        PACKET_HANDLER.registerMessage(messageID, messageType, encoder, decoder, messageConsumer);
        messageID++;
    }

    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<>();

    public static void queueServerWork(int tick, Runnable action) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            workQueue.add(new AbstractMap.SimpleEntry<>(action, tick));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return; // Limitez à une phase.

        // Récupérez l'instance du serveur depuis l'événement
        MinecraftServer server = event.getServer();
        if (server == null) return;

        // Accédez au monde principal (Overworld)
//        ServerLevel overworld = server.overworld();
//        if (overworld != null) {
//            updateAllRoads(overworld);
//        }
    }




}