package fr.renblood.npcshopkeeper.init;

import fr.renblood.npcshopkeeper.Npcshopkeeper;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityInit {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Npcshopkeeper.MODID);



    public static final RegistryObject<EntityType<TradeNpcEntity>> TRADE_NPC_ENTITY = ENTITY_TYPES.register(
            "trade_npc",
            () -> EntityType.Builder.of(TradeNpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.0F)
                    .build("trade_npc")
    );


}