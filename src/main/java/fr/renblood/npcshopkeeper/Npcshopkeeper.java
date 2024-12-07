package fr.renblood.npcshopkeeper;
import fr.renblood.npcshopkeeper.client.renderer.TradeNpcRenderer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.init.NpcshopkeeperModMenus;
import fr.renblood.npcshopkeeper.world.WorldEventHandler;
import fr.renblood.npcshopkeeper.world.inventory.CreateTradeMenu;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;


import java.util.function.Supplier;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.AbstractMap;

import static fr.renblood.npcshopkeeper.init.EntityInit.ENTITY_TYPES;
import static fr.renblood.npcshopkeeper.init.EntityInit.TRADE_NPC_ENTITY;

// The value here should match an entry in the META-INF/mods.toml file

@Mod("npcshopkeeper")
public class Npcshopkeeper {
    public static final Logger LOGGER = LogManager.getLogger(Npcshopkeeper.class);
    public static final String MODID = "npcshopkeeper";
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);




    @SubscribeEvent
    public static void onServerStarting(ServerStartedEvent event) {
        JsonTradeFileManager.onServerStarted(event);
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
        NpcshopkeeperModMenus.REGISTRY.register(bus);


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

//    @SubscribeEvent
//    public void tick(TickEvent.ServerTickEvent event) {
//        if (event.phase == TickEvent.Phase.END) {
//            List<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
//            workQueue.forEach(work -> {
//                work.setValue(work.getValue() - 1);
//                if (work.getValue() == 0)
//                    actions.add(work);
//            });
//            actions.forEach(e -> e.getKey().run());
//            workQueue.removeAll(actions);
//        }
//    }
//
//    @SubscribeEvent
//    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
//        LOGGER.info("Attempting to register attributes for TradeNpcEntity.");
//        if (TRADE_NPC_ENTITY.get() != null) {
//            event.put(TRADE_NPC_ENTITY.get(), TradeNpcEntity.createAttributes().build());
//            LOGGER.info("Successfully registered attributes for TradeNpcEntity.");
//        } else {
//            LOGGER.error("Failed to register attributes: TRADE_NPC_ENTITY is null!");
//        }
//    }



}