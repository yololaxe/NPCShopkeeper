package fr.renblood.npcshopkeeper.services;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
public final class ShopWandItem extends Item {
    public ShopWandItem(){super(new Item.Properties().stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext c){if(c.getPlayer() instanceof ServerPlayer p)WorkshopServer.create(p,c.getClickedPos().relative(c.getClickedFace()));return InteractionResult.sidedSuccess(c.getLevel().isClientSide);}
}
