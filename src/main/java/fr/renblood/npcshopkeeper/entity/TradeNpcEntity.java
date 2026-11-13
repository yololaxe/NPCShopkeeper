package fr.renblood.npcshopkeeper.entity;

import fr.renblood.npcshopkeeper.command.OpenTravelGuiCommand;
import fr.renblood.npcshopkeeper.api.NpcShopkeeperAPI;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import fr.renblood.npcshopkeeper.data.io.JsonRepository;
import fr.renblood.npcshopkeeper.data.npc.TradeNpc;
import fr.renblood.npcshopkeeper.data.trade.Trade;
import fr.renblood.npcshopkeeper.data.trade.TradeHistory;
import fr.renblood.npcshopkeeper.manager.harbor.TravelManager;
import fr.renblood.npcshopkeeper.manager.data.RuntimeDataCache;
import fr.renblood.npcshopkeeper.manager.npc.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.quest.QuestNpcInteractionManager;
import fr.renblood.npcshopkeeper.manager.server.OnServerStartedManager;
import fr.renblood.npcshopkeeper.network.PacketHandler;
import fr.renblood.npcshopkeeper.network.SyncDepartureTimePacket;
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

public class TradeNpcEntity extends Villager {
    private static final Logger LOGGER = LogManager.getLogger(TradeNpcEntity.class);
    private static final EntityDataAccessor<String> TEXTURE =
            SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_CAPTAIN =
            SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> PORT_NAME =
            SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> API_NPC_SPAWN_ID =
            SynchedEntityData.defineId(TradeNpcEntity.class, EntityDataSerializers.STRING);

    private static final String INN_CUSTOMER_TEXTURE = "RenbloodInnCustomerTexture";

    private TradeNpc tradeNpc;
    private String npcId;
    private String npcName;
    private List<String> texts = Collections.emptyList();
    private BlockPos position;
    private String tradeCategory;
    private Trade trade;
    private TradeHistory tradeHistory;
    private Player gazePlayer;
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
        this.entityData.define(IS_CAPTAIN, false);
        this.entityData.define(PORT_NAME, "");
        this.entityData.define(API_NPC_SPAWN_ID, "");
    }
    
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isNoAi() && !isInvisible() && (isCaptain() || getPersistentData().contains("RenbloodShopId"))) {
            if (gazePlayer != null && (!gazePlayer.isAlive() || gazePlayer.isRemoved() || gazePlayer.level() != level() || distanceToSqr(gazePlayer) > 64)) gazePlayer = null;
            if (tickCount == 1 || tickCount % 10 == 0) {
                gazePlayer = level().getNearestPlayer(this, 8);
                if (gazePlayer != null && (gazePlayer.isSpectator() || !hasLineOfSight(gazePlayer))) gazePlayer = null;
            }
            if (gazePlayer != null) {
                turnTowards(gazePlayer);
            }
        }

        if (!level().isClientSide && tradeNpc == null && isInnCustomer()
                && (tickCount == 1 || tickCount % 100 == 0)) {
            restoreInnCustomer("");
        }

    }
    


    private void turnTowards(Player player) {
                double dx = player.getX() - getX(), dz = player.getZ() - getZ();
                double dy = player.getEyeY() - getEyeY();
                float yaw = (float)(Math.atan2(dz, dx) * 180 / Math.PI) - 90;
                float pitch = (float)(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
                setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(), yaw, 8));
                setYHeadRot(getYRot()); setYBodyRot(getYRot());
                setXRot(net.minecraft.util.Mth.approachDegrees(getXRot(), net.minecraft.util.Mth.clamp(pitch, -40, 40), 6));
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
        
        if (this.isCaptain()) {
            this.entityData.set(TEXTURE, "textures/entity/captain.png");
            this.setCustomName(Component.literal("Captain " + this.getPortName()));
            this.setCustomNameVisible(true);
        } else if (!isApiManaged() && tag != null && tag.hasUUID("TradeNpcId")) {
            applyModelById(tag.getUUID("TradeNpcId"));
        }
        return result;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsCaptain", this.isCaptain());
        tag.putString("PortName", this.getPortName());
        
        if (getPersistentData().contains("RenbloodShopId")) {
            tag.putString("RenbloodShopSkin",getTexture());
        } else if (isInnCustomer()) {
            if (tradeNpc != null && !getTexture().isBlank()) {
                tag.putString(INN_CUSTOMER_TEXTURE, getTexture());
            }
        } else if (this.getPersistentData().getBoolean("IsDecor")) {
            tag.putBoolean("IsDecor", true);
            tag.putString("DecorName", this.getPersistentData().getString("DecorName"));
        } else if (!isApiManaged() && npcId != null) {
            tag.putUUID("TradeNpcId", UUID.fromString(npcId));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("IsCaptain")) {
            this.setCaptain(tag.getBoolean("IsCaptain"));
        }
        if (tag.contains("PortName")) {
            this.setPortName(tag.getString("PortName"));
        }
        String apiNpcSpawnId = this.getPersistentData().getString("ApiNpcSpawnId");
        if (!apiNpcSpawnId.isBlank()) {
            this.entityData.set(API_NPC_SPAWN_ID, apiNpcSpawnId);
        }

        if (this.isCaptain()) {
            this.setCustomName(Component.literal("Captain " + this.getPortName()));
            this.setCustomNameVisible(true);
            this.entityData.set(TEXTURE, "textures/entity/captain.png");
            return;
        }

        if (getPersistentData().contains("RenbloodShopId")) {
            fr.renblood.npcshopkeeper.services.WorkshopServer.restore(this);
            return;
        }
        // Les PNJ API sont reconstruits par GlobalNpcSpawnManager et n'existent pas dans trades_npcs.json.
        if (isApiManaged()) {
            return;
        }
        
        if (isInnCustomer() || tag.getCompound("ForgeData").getBoolean(NpcShopkeeperAPI.INN_CUSTOMER_TAG)) {
            String texture = tag.getString(INN_CUSTOMER_TEXTURE);
            if (!texture.isBlank()) restoreInnCustomer(texture);
            return;
        }

        if (tag.getBoolean("IsDecor")) {
            String decorName = tag.getString("DecorName");
            if (!decorName.isEmpty()) {
                Map<String, Object> npcData = GlobalNpcManager.getNpcData(decorName);
                if (npcData != null) {
                    TradeNpc model = new TradeNpc(decorName, npcData, "decor", this.blockPosition());
                    this.setTradeNpc(model);
                }
            }
            return;
        }

        if (tag.hasUUID("TradeNpcId")) {
            applyModelById(tag.getUUID("TradeNpcId"));
        }
    }

    private void applyModelById(UUID id) {
        if (this.isCaptain()) return;

        String path = OnServerStartedManager.PATH_NPCS;
        if (path == null) {
            LOGGER.error("[TradeNpcEntity] Impossible de charger le modèle : chemin trades_npcs.json non initialisé.");
            return;
        }

        TradeNpc model = RuntimeDataCache.getNpc(id);

        if (model != null) {
            setTradeNpc(model);
        } else {
            LOGGER.warn("[TradeNpcEntity] No model for UUID {}", id);
        }
    }

    public boolean isInnCustomer() {
        return getPersistentData().getBoolean(NpcShopkeeperAPI.INN_CUSTOMER_TAG);
    }

    private void restoreInnCustomer(String texture) {
        Component name = getCustomName();
        if (name == null || name.getString().isBlank()) return;
        if (texture.isBlank()) {
            var template = GlobalNpcManager.getNpcData(name.getString());
            if (template == null || !(template.get("Texture") instanceof String original) || original.isBlank()) return;
            texture = original;
        }
        var model = new TradeNpc(getStringUUID(), name.getString(), Map.of("Texture", texture),
                "inn_customer", blockPosition());
        model.setRouteNpc(true);
        applyTradeNpcModel(model);
    }

    public void setTradeNpc(TradeNpc model) {
        applyTradeNpcModel(model);
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

    private void applyTradeNpcModel(TradeNpc model) {
        this.tradeNpc = model;
        this.npcId = model.getNpcId();
        this.npcName = model.getNpcName();
        this.position = model.getPos();
        this.tradeCategory = model.getTradeCategory();
        this.texts = model.getTexts() != null
                ? new ArrayList<>(model.getTexts())
                : Collections.emptyList();

        this.entityData.set(TEXTURE, model.getTexture());
        this.setCustomName(Component.literal(this.npcName));
        this.setCustomNameVisible(true);
    }

    public TradeNpc getTradeNpc() {
        return this.tradeNpc;
    }

    public boolean isCaptain() {
        return this.entityData.get(IS_CAPTAIN);
    }

    public void setCaptain(boolean isCaptain) {
        this.entityData.set(IS_CAPTAIN, isCaptain);
        if (isCaptain) {
            this.entityData.set(TEXTURE, "textures/entity/captain.png");
            if (this.getPortName() != null && !this.getPortName().isEmpty()) {
                this.setCustomName(Component.literal("Captain " + this.getPortName()));
                this.setCustomNameVisible(true);
            }
        }
    }

    public String getPortName() {
        return this.entityData.get(PORT_NAME);
    }

    public void setPortName(String portName) {
        this.entityData.set(PORT_NAME, portName);
        if (this.isCaptain()) {
            this.setCustomName(Component.literal("Captain " + portName));
            this.setCustomNameVisible(true);
        }
    }

    private boolean isApiManaged() {
        return !this.entityData.get(API_NPC_SPAWN_ID).isBlank()
                || !this.getPersistentData().getString("ApiNpcSpawnId").isBlank();
    }

    public String getApiNpcSpawnId() {
        return this.entityData.get(API_NPC_SPAWN_ID);
    }

    public void setApiNpcSpawnId(String spawnId) {
        String value = spawnId == null ? "" : spawnId;
        this.entityData.set(API_NPC_SPAWN_ID, value);
        if (!value.isBlank()) {
            this.getPersistentData().putString("ApiNpcSpawnId", value);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {

            if (getPersistentData().contains("RenbloodShopId")) {
                fr.renblood.npcshopkeeper.services.WorkshopServer.open(serverPlayer,this,serverPlayer.isShiftKeyDown());
                return InteractionResult.SUCCESS;
            }
            if (this.isCaptain()) {
                fr.renblood.npcshopkeeper.services.VoyageService.open(serverPlayer,getPortName(),serverPlayer.isShiftKeyDown()&&serverPlayer.hasPermissions(2));
                return InteractionResult.SUCCESS;
            }

            CompoundTag persistentData = this.getPersistentData();
            if (persistentData.getBoolean(NpcShopkeeperAPI.INN_CUSTOMER_TAG)) {
                return NpcShopkeeperAPI.handleInnCustomerInteraction(serverPlayer, this)
                        ? InteractionResult.SUCCESS
                        : InteractionResult.PASS;
            }

            if (QuestNpcInteractionManager.handle(serverPlayer, List.of(), persistentData.getString("ApiNpcSpawnId"),
                    this.getNpcName(), this.getTexture())) {
                return InteractionResult.SUCCESS;
            }

            if (persistentData.contains("ApiNpcSpawnId")
                    && !persistentData.getBoolean("ApiIsShopkeeper")) {
                return InteractionResult.SUCCESS;
            }
            
            if (fr.renblood.npcshopkeeper.services.ContractService.open(serverPlayer,this)) return InteractionResult.SUCCESS;
            if (this.texts != null && !this.texts.isEmpty()) {
                String randomText = this.texts.get(RandomSource.create().nextInt(this.texts.size()));
                serverPlayer.displayClientMessage(Component.literal(randomText), false);
            } else {
                serverPlayer.displayClientMessage(Component.translatable("gui.npcshopkeeper.trade.no_text"), false);
            }

            try {
                if (OnServerStartedManager.PATH_HISTORY == null || OnServerStartedManager.PATH == null) {
                    LOGGER.error("Chemins JSON non initialisés dans mobInteract !");
                    serverPlayer.displayClientMessage(Component.translatable("command.npcshopkeeper.error.internal", "Chemins non chargés"), true);
                    return InteractionResult.FAIL;
                }

                Optional<TradeHistory> ongoing = RuntimeDataCache.getOpenHistory(this.npcId);

                if (ongoing.isPresent()) {
                    this.tradeHistory = ongoing.get();
                    String existingTradeName = this.tradeHistory.getTradeName();
                    TradeCommandProcedure.execute(
                            this.level(),
                            serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            existingTradeName,
                            serverPlayer,
                            this.npcId,
                            this.npcName
                    );
                } else {
                    if (this.tradeCategory != null && !this.tradeCategory.isEmpty()) {
                        List<Trade> available = RuntimeDataCache.getTradesByCategory(this.tradeCategory);

                        if (!available.isEmpty()) {
                            this.trade = available.get(RandomSource.create().nextInt(available.size()));
                            TradeCommandProcedure.execute(
                                    this.level(),
                                    serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                                    this.trade.getName(),
                                    serverPlayer,
                                    this.npcId,
                                    this.npcName
                            );
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Erreur dans mobInteract de TradeNpcEntity", e);
                return InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public void rewardTradeXp(MerchantOffer offer) {
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
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void travel(Vec3 movement) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

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
