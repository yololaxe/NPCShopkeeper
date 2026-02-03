package fr.renblood.npcshopkeeper.network;

import fr.renblood.npcshopkeeper.data.harbor.Port;
import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import fr.renblood.npcshopkeeper.manager.harbor.PortManager;
import fr.renblood.npcshopkeeper.manager.harbor.TravelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
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

            // Calcul du coût réel
            double distance = Math.sqrt(player.blockPosition().distSqr(destPort.getPos()));
            int blocksPerIron = PortManager.getBlocksPerIron();
            int costInIron = (int) Math.ceil(distance / (double)blocksPerIron);
            costInIron = Math.max(1, costInIron);

            // Récupération des items
            Item ironCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "iron_coin"));
            Item bronzeCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "bronze_coin"));
            Item silverCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "silver_coin"));
            Item goldCoin = ForgeRegistries.ITEMS.getValue(new ResourceLocation("medieval_coins", "gold_coin"));

            if (ironCoin == null || bronzeCoin == null || silverCoin == null || goldCoin == null) {
                player.displayClientMessage(Component.literal("Erreur : Items medieval_coins introuvables."), true);
                return;
            }

            // Calcul richesse
            long totalWealth = 0;
            totalWealth += player.getInventory().countItem(ironCoin);
            totalWealth += player.getInventory().countItem(bronzeCoin) * 64L;
            totalWealth += player.getInventory().countItem(silverCoin) * 64L * 64L;
            totalWealth += player.getInventory().countItem(goldCoin) * 64L * 64L * 64L;

            if (totalWealth >= costInIron) {
                // Paiement
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == ironCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == bronzeCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == silverCoin, -1, player.inventoryMenu.getCraftSlots());
                player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == goldCoin, -1, player.inventoryMenu.getCraftSlots());

                long remainingWealth = totalWealth - costInIron;
                
                // Rendu monnaie
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

                // Inscription au voyage au lieu de téléportation immédiate
                // Trouver le capitaine le plus proche pour identifier le port de départ
                AABB checkArea = new AABB(player.blockPosition()).inflate(10);
                TradeNpcEntity captain = player.level().getEntitiesOfClass(TradeNpcEntity.class, checkArea, e -> e.isCaptain()).stream().findFirst().orElse(null);
                
                if (captain != null) {
                    String fromPort = captain.getPortName();
                    TravelManager.registerPassenger(player, fromPort, destPort);
                    
                    // Calcul du temps restant (6000 - temps actuel)
                    long currentTime = player.level().getDayTime() % 24000;
                    long departureTime = 6000; // Départ à 6000 ticks
                    long timeRemaining = departureTime - currentTime;
                    
                    // Envoi du temps de départ au client pour le HUD
                    PacketHandler.sendToPlayer(new SyncDepartureTimePacket(departureTime), player);
                    
                    player.displayClientMessage(Component.literal("✅ Billet acheté ! Départ prévu dans " + (timeRemaining / 20) + " secondes."), true);
                } else {
                    player.displayClientMessage(Component.literal("Erreur : Capitaine introuvable pour valider le départ."), true);
                }

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
