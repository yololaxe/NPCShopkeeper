package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;
import java.util.function.Supplier;

public class TravelPacket {
    private final String destinationPortName;
    private final int clientEstimatedCost;

    public TravelPacket(String destinationPortName, int clientEstimatedCost) {
        this.destinationPortName = destinationPortName;
        this.clientEstimatedCost = clientEstimatedCost;
    }

    public TravelPacket(FriendlyByteBuf buf) {
        this.destinationPortName = buf.readUtf();
        this.clientEstimatedCost = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(destinationPortName);
        buf.writeInt(clientEstimatedCost);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Optional<Port> destPortOpt = PortManager.getPort(destinationPortName);
            if (destPortOpt.isEmpty()) {
                player.displayClientMessage(Component.literal("Erreur : Port de destination introuvable."), true);
                return;
            }
            Port destPort = destPortOpt.get();

            // Calcul du coût réel en "Iron Coins" avec le prix configuré
            double distance = Math.sqrt(player.blockPosition().distSqr(destPort.getPos()));
            int blocksPerIron = PortManager.getBlocksPerIron();
            int costInIron = (int) Math.ceil(distance / (double)blocksPerIron);
            costInIron = Math.max(1, costInIron);

            // Récupération des items du mod medieval_coins
            Item ironCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "iron_coin"));
            Item bronzeCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "bronze_coin"));
            Item silverCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "silver_coin"));
            Item goldCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "gold_coin"));

            if (ironCoin == null || bronzeCoin == null || silverCoin == null || goldCoin == null) {
                player.displayClientMessage(Component.literal("Erreur : Items medieval_coins introuvables."), true);
                return;
            }

            // Calcul de la richesse totale du joueur en "Iron Coins"
            long totalWealth = 0;
            totalWealth += player.getInventory().countItem(ironCoin);
            totalWealth += player.getInventory().countItem(bronzeCoin) * 64L;
            totalWealth += player.getInventory().countItem(silverCoin) * 64L * 64L;
            totalWealth += player.getInventory().countItem(goldCoin) * 64L * 64L * 64L;

            if (totalWealth >= costInIron) {
                // Paiement : On retire le montant exact en convertissant si nécessaire
                
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ironCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == bronzeCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == silverCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == goldCoin, -1, player.inventoryMenu.getCraftSlots());

                long remainingWealth = totalWealth - costInIron;
                
                // Redistribution du reste
                long goldToGive = remainingWealth / (64 * 64 * 64);
                remainingWealth %= (64 * 64 * 64);
                
                long silverToGive = remainingWealth / (64 * 64);
                remainingWealth %= (64 * 64);
                
                long bronzeToGive = remainingWealth / 64;
                remainingWealth %= 64;
                
                long ironToGive = remainingWealth;

                giveItems(player, goldCoin, (int) goldToGive);
                giveItems(player, silverCoin, (int) silverToGive);
                giveItems(player, bronzeCoin, (int) bronzeToGive);
                giveItems(player, ironCoin, (int) ironToGive);

                // Téléportation
                player.teleportTo(
                        player.server.getLevel(player.level().dimension()),
                        destPort.getPos().getX() + 0.5,
                        destPort.getPos().getY(),
                        destPort.getPos().getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                );
                player.displayClientMessage(Component.literal("⚓ Arrivée à " + destPort.getName() + " !"), true);
            } else {
                player.displayClientMessage(Component.literal("Pas assez de pièces ! Coût : " + formatCost(costInIron)), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void giveItems(ServerPlayer player, Item item, int count) {
        while (count > 0) {
            int stackSize = Math.min(count, 64);
            player.getInventory().add(new ItemStack(item, stackSize));
            count -= stackSize;
        }
    }

    private String formatCost(int ironCost) {
        if (ironCost >= 64 * 64 * 64) return (ironCost / (64 * 64 * 64)) + " Gold";
        if (ironCost >= 64 * 64) return (ironCost / (64 * 64)) + " Silver";
        if (ironCost >= 64) return (ironCost / 64) + " Bronze";
        return ironCost + " Iron";
    }
}
