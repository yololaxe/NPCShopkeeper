package fr.renblood.npcshopkeeper.entity;

import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.procedures.trade.TradeCommandProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager.*;

public class TradeNpcEntity extends Villager {
    private static final Logger LOGGER = LogManager.getLogger(TradeNpcEntity.class);
    private static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.STRING);

    private TradeNpc tradeNpc;
    private String npcId;
    private String npcName;
    private List<String> texts = Collections.emptyList();
    private BlockPos position;
    private String tradeCategory;
    private Trade trade;
    private TradeHistory tradeHistory;
    private boolean initialized = false;
    private boolean created = false;

    public TradeNpcEntity(EntityType<? extends TradeNpcEntity> type, Level world) {
        super(type, world);
        this.setCustomNameVisible(true);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TEXTURE, "textures/entity/banker.png");
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor world,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            SpawnGroupData data,
            CompoundTag tag
    ) {
        SpawnGroupData result = super.finalizeSpawn(world, difficulty, reason, data, tag);
        if (tag != null && tag.hasUUID("TradeNpcId")) {
            applyModelById(tag.getUUID("TradeNpcId"));
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (npcId != null) {
            tag.putUUID("TradeNpcId", UUID.fromString(npcId));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("TradeNpcId")) {
            applyModelById(tag.getUUID("TradeNpcId"));
        }
    }

    private void applyModelById(UUID id) {
        TradeNpc model = new JsonRepository<>(
                Paths.get(PATH_NPCS),
                "npcs",
                TradeNpc::fromJson,
                TradeNpc::toJson
        ).loadAll().stream()
                .filter(n -> n.getNpcId().equals(id.toString()))
                .findFirst().orElse(null);

        if (model != null) {
            setTradeNpc(model);
        } else {
            LOGGER.warn("[TradeNpcEntity] No model for UUID {}", id);
        }
    }

    /**
     * Associe un modèle TradeNpc à cette entité,
     * initialise nom, texture, position, textes, etc.
     */
    public void setTradeNpc(TradeNpc model) {
        this.tradeNpc = model;
        this.npcId = model.getNpcId();
        this.npcName = model.getNpcName();
        this.position = model.getPos();
        this.tradeCategory = model.getTradeCategory();
        this.texts = model.getTexts() != null
                ? new ArrayList<>(model.getTexts())
                : Collections.emptyList();

        // applique la texture
        this.entityData.set(TEXTURE, model.getTexture());
        this.setCustomName(Component.literal(this.npcName));
        this.setCustomNameVisible(true);
        this.moveTo(
                this.position.getX() + 0.5D,
                this.position.getY(),
                this.position.getZ() + 0.5D,
                this.getYRot(),
                this.getXRot()
        );

        LOGGER.info("[TradeNpcEntity] setTradeNpc() id={} name='{}' tex='{}'",
                this.npcId, this.npcName, model.getTexture());
    }

    /**
     * Récupère le modèle TradeNpc associé.
     */
    public TradeNpc getTradeNpc() {
        return this.tradeNpc;
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
                        Paths.get(PATH_HISTORY),
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
                                Paths.get(PATH),
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
        // pas de xp
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D);
    }

    public static boolean canSpawn(
            EntityType<TradeNpcEntity> type,
            LevelAccessor level,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random
    ) {
        return Villager.checkMobSpawnRules(type, level, reason, pos, random);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public void travel(Vec3 movement) {
        // reste en place
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
        // pas de push
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    // getters

    public String getNpcId()      { return npcId; }
    public String getNpcName()    { return npcName; }
    public String getTexture()    { return this.entityData.get(TEXTURE); }
    public List<String> getTexts(){ return texts; }
    public String getTradeCategory() { return tradeCategory; }
    public Trade getTrade()       { return trade; }
    public TradeHistory getTradeHistory() { return tradeHistory; }
    public boolean isInitialized(){ return initialized; }
    public boolean isCreated()    { return created; }
}
