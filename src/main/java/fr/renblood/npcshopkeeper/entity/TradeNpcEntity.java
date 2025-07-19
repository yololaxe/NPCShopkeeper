package fr.renblood.npcshopkeeper.entity;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.nio.file.Paths;
import java.util.*;

;

public class TradeNpcEntity extends Villager {

    private String npcId;
    private String npcName;
    Map<String, Object> npcData; //texture texts
    private ArrayList<String> texts;
    private BlockPos position;
    private String tradeCategory; // Catégorie du trade assignée à ce PNJ
    private Trade trade;
    private TradeHistory tradeHistory;
    private boolean initialized = false;
    private boolean created = false;
    private TradeNpc tradeNpc;

    private static final Logger LOGGER = LogManager.getLogger(TradeNpcEntity.class);

    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.STRING);

    public TradeNpcEntity(EntityType<? extends TradeNpcEntity> type, Level world) {
        super(type, world);
    }

    public void setTradeNpc(TradeNpc tradeNpc) {
        this.tradeNpc = tradeNpc;
        this.initializeNpcData();

    }

    public void initializeNpcData() {
        if (tradeNpc == null) {
            LOGGER.error("Impossible de récupérer les données du TradeNpc.");
            return;
        }

        this.setNpcId(this.getUUID().toString());
        this.setNpcName(tradeNpc.getNpcName());
        this.position = tradeNpc.getPos();
        this.tradeCategory = tradeNpc.getTradeCategory();
        this.texts = tradeNpc.getTexts();

        // 💥 C'est ce qui manque actuellement !
        this.setTexture(tradeNpc.getTexture());

        this.setCustomName(Component.literal(this.npcName));
        this.setCustomNameVisible(true);
        this.setPos(this.position.getX(), this.position.getY(), this.position.getZ());

        LOGGER.info("PNJ initialisé : " + this.npcName + " | Texture : " + this.getTexture());
    }



    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE, "textures/entity/banker.png"); // valeur par défaut
    }


    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.npcName != null) {
            GlobalNpcManager.deactivateNpc(this.npcName);
        }

        // on charge tous les TradeNpc, on filtre celui qu'on supprime, puis on réécrit
        JsonRepository<TradeNpc> npcRepo = new JsonRepository<>(
                Paths.get(JsonFileManager.pathNpcs),
                "npcs",
                TradeNpc::fromJson,
                TradeNpc::toJson
        );
        var remaining = npcRepo.loadAll().stream()
                .filter(n -> !n.getNpcId().equals(this.getNpcId()))
                .toList();
        npcRepo.saveAll(remaining);
    }


    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        // Affichage d’un texte aléatoire si défini
        if (this.texts != null && !this.texts.isEmpty()) {
            String randomText = this.texts.get(RandomSource.create().nextInt(this.texts.size()));
            player.displayClientMessage(Component.literal(randomText), false);
        } else {
            player.displayClientMessage(Component.literal("Ce PNJ n'a rien à dire pour le moment."), false);
        }

        // Côté serveur : gérer le trade
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            try {
                // 1) Charger l’historique des trades
                JsonRepository<TradeHistory> historyRepo = new JsonRepository<>(
                        Paths.get(JsonFileManager.pathHistory),
                        "history",
                        TradeHistory::fromJson,
                        TradeHistory::toJson
                );
                List<TradeHistory> histories = historyRepo.loadAll();

                // 2) Chercher un trade non terminé pour ce PNJ
                Optional<TradeHistory> ongoing = histories.stream()
                        .filter(th -> !th.isFinished() && th.getNpcId().equals(this.npcId))
                        .findFirst();

                if (ongoing.isPresent()) {
                    // Trade existant : reprendre
                    this.tradeHistory = ongoing.get();
                    String existingTradeName = this.tradeHistory.getTradeName();
                    serverPlayer.displayClientMessage(
                            Component.literal("Reprise du trade : " + existingTradeName),
                            true
                    );
                    TradeCommandProcedure.execute(
                            this.level(),
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            existingTradeName,
                            serverPlayer,
                            this.npcId,
                            this.npcName
                    );
                } else {
                    // Nouveau trade : en choisir un au hasard dans la catégorie
                    if (this.tradeCategory != null && !this.tradeCategory.isEmpty()) {
                        JsonRepository<Trade> tradeRepo = new JsonRepository<>(
                                Paths.get(JsonFileManager.path),
                                "trades",
                                Trade::fromJson,
                                Trade::toJson
                        );
                        List<Trade> available = tradeRepo.loadAll().stream()
                                .filter(t -> t.getCategory().equalsIgnoreCase(this.tradeCategory))
                                .toList();

                        if (!available.isEmpty()) {
                            this.trade = available.get(RandomSource.create().nextInt(available.size()));
                            serverPlayer.displayClientMessage(
                                    Component.literal("Nouveau trade : " + this.trade.getName()),
                                    true
                            );
                            TradeCommandProcedure.execute(
                                    this.level(),
                                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                    this.trade.getName(),
                                    serverPlayer,
                                    this.npcId,
                                    this.npcName
                            );
                        } else {
                            serverPlayer.displayClientMessage(
                                    Component.literal("Aucun trade dans la catégorie : " + this.tradeCategory),
                                    true
                            );
                        }
                    } else {
                        serverPlayer.displayClientMessage(
                                Component.literal("Ce PNJ n'a pas de catégorie de trade assignée."),
                                true
                        );
                    }
                }
            } catch (Exception e) {
                serverPlayer.displayClientMessage(
                        Component.literal("Erreur lors de l'échange : " + e.getMessage()),
                        true
                );
                LOGGER.error("Erreur dans mobInteract de TradeNpcEntity", e);
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }



    @Override
    public void rewardTradeXp(MerchantOffer offer) {
        // Implémentation vide ou logique personnalisée
        // Exemple : Ne rien faire pour le PNJ
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)  // Santé max
                .add(Attributes.MOVEMENT_SPEED, 0.0D)  // Désactiver la vitesse de mouvement
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)  // Résistance totale aux chocs
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)  // Résistance totale aux chocs
                .add(Attributes.ATTACK_DAMAGE, 0.0D)  // Désactiver les attaques
                .add(Attributes.FOLLOW_RANGE, 0.0D);  // Désactiver la portée de suivi
    }

    public static boolean canSpawn(EntityType<TradeNpcEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random) {
        return Villager.checkMobSpawnRules(entityType, level, spawnType, position, random);
    }


    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Bloquer tous les dégâts, même les dégâts magiques ou de chute
        return true;
    }

    @Override
    public void travel(Vec3 movement) {
        // Empêche les mouvements de l'entité
        if (!this.level().isClientSide) {
            super.travel(Vec3.ZERO);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // Ne rien faire pour empêcher les interactions physiques
    }

    @Override
    public boolean isNoGravity() {
        return true; // Empêche l'entité d'être affectée par la gravité
    }

    // Propriétés spécifiques au PNJ
    public String getNpcId() {
        return npcId;
    }

    public void setNpcId(String npcId) {
        this.npcId = npcId;
    }

    public String getNpcName() {
        return npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    public ArrayList<String> getTexts() {
        return texts;
    }

    public void setTexts(ArrayList<String> texts) {
        this.texts = texts;
    }

    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    public void setTexture(String texture) {
        this.entityData.set(TEXTURE, texture);
    }


    public BlockPos getPosition() {
        return position;
    }

    public void setPosition(BlockPos position) {
        this.position = position;
    }

    public void setTradeCategory(String category) {
        this.tradeCategory = category;
    }

    public Map<String, Object> getNpcData() {
        return npcData;
    }

    public String getTradeCategory() {
        return tradeCategory;
    }

    public Trade getTrade() {
        return trade;
    }

    public TradeHistory getTradeHistory() {
        return tradeHistory;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public void setTradeHistory(TradeHistory tradeHistory) {
        this.tradeHistory = tradeHistory;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public boolean isCreated() {
        return created;
    }

    public void setCreated(boolean created) {
        this.created = created;
    }


}