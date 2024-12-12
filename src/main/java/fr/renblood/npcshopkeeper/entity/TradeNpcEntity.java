package fr.renblood.npcshopkeeper.entity;

import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.Trade;
import fr.renblood.npcshopkeeper.data.TradeHistory;
import fr.renblood.npcshopkeeper.manager.GlobalNpcManager;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.procedures.TradeCommandProcedure;
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

import java.util.*;

;

public class TradeNpcEntity extends Villager {

    private String npcId = this.stringUUID;
    private String npcName = GlobalNpcManager.getRandomInactiveNpc().get();
    Map<String, Object> npcData = GlobalNpcManager.getNpcData(npcName);
    private ArrayList<String> texts = (ArrayList<String>) npcData.getOrDefault("Texts", new ArrayList<>());
    private String texture = (String) npcData.getOrDefault("Texture", "textures/entity/banker.png");
    private BlockPos position;
    private String tradeCategory; // Catégorie du trade assignée à ce PNJ
    private Trade trade;
    private TradeHistory tradeHistory;
    private boolean initialized = false;

    private static final Logger LOGGER = LogManager.getLogger(TradeCommandProcedure.class);

    public TradeNpcEntity(EntityType<TradeNpcEntity> type, Level world) {
        super(type, world);
        if (world.isClientSide()) {
            LOGGER.warn("TradeNpcEntity constructor called on client side. Ignoring...");
            return;
        }

//        this.registerGoals();
        if (!initialized) {
            LOGGER.info("Initializing NPC entity: {}", this.getUUID());

            initialized = true;
        } else {
            LOGGER.warn("TradeNpcEntity was already initialized: {}", this.getUUID());
        }
    }

    public boolean initializeNpcData() {
        if (initialized) {
            LOGGER.warn("initializeNpcData() déjà appelée pour l'entité : " + this.getUUID());
            return false;
        }

        Optional<String> npcNameOptional = GlobalNpcManager.getRandomInactiveNpc();
        if (npcNameOptional.isEmpty()) {
            LOGGER.error("Tous les PNJs disponibles sont déjà en jeu !");
            return false;
        }

        GlobalNpcManager.activateNpc(npcName);
        this.setCustomName(Component.literal(this.npcName));
        this.setCustomNameVisible(true);

        LOGGER.info("PNJ initialisé avec succès : " + this.npcName + " --> " + this.texture);
        initialized = true;
        return true;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.npcName != null) {
            GlobalNpcManager.deactivateNpc(this.npcName);
        }
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
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
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

    public void setTrade(Trade trade) {
        this.trade = trade;
    }


    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        System.out.println("Début de la méthode mobInteract");
        if (this.texts != null && !this.texts.isEmpty()) {
            // Sélectionner un texte aléatoire
            String randomText = this.texts.get(new Random().nextInt(this.texts.size()));
            player.displayClientMessage(Component.literal(randomText), false);
        } else {
            player.displayClientMessage(Component.literal("Ce PNJ n'a rien à dire pour le moment."), false);
        }
        if (!this.level().isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    // Vérifier si un trade existe déjà pour ce PNJ
                    Pair<Boolean, TradeHistory> tradeStatus = JsonTradeFileManager.checkTradeStatusForNpc(this.npcId);
                    position = serverPlayer.blockPosition();

                    if (tradeStatus.first) {
                        // Si un trade existe, récupérer ses détails
                        tradeHistory = tradeStatus.second;
                        if (tradeHistory == null || tradeHistory.getTradeName() == null || tradeHistory.getTradeItems().isEmpty()) {
                            serverPlayer.displayClientMessage(Component.literal("Erreur : données de trade manquantes pour le PNJ."), true);
                            LOGGER.error("Données de trade manquantes pour le PNJ : " + this.npcId);
                            return InteractionResult.FAIL;
                        }

                        // Utiliser le trade existant
                        String existingTradeName = tradeHistory.getTradeName();
                        serverPlayer.displayClientMessage(Component.literal("Un trade existant a été trouvé : " + existingTradeName), true);


                        TradeCommandProcedure.execute(
                                this.level(), position.getX(), position.getY(), position.getZ(),
                                existingTradeName, serverPlayer, this.npcId, this.npcName
                        );
                    } else {
                        // Sinon, créer un nouveau trade aléatoire
                        if (this.tradeCategory != null) {
                            List<Trade> trades = JsonTradeFileManager.getTradesByCategory(this.tradeCategory);

                            if (!trades.isEmpty()) {
                                trade = trades.get(new Random().nextInt(trades.size()));
                                serverPlayer.displayClientMessage(Component.literal("Nouveau trade généré : " + trade.getName()), true);


                                TradeCommandProcedure.execute(
                                        this.level(), position.getX(), position.getY(), position.getZ(),
                                        trade.getName(), serverPlayer, this.npcId, this.npcName
                                );
                            } else {
                                serverPlayer.displayClientMessage(Component.literal("Aucun trade trouvé dans cette catégorie : " + tradeCategory), true);
                            }
                        } else {
                            serverPlayer.displayClientMessage(Component.literal("Ce PNJ n'a pas de catégorie de trade assignée."), true);
                        }
                    }
                } catch (Exception e) {
                    serverPlayer.displayClientMessage(Component.literal("Erreur lors de l'exécution de TradeCommandProcedure : " + e.getMessage()), true);
                    e.printStackTrace();
                    return InteractionResult.FAIL;
                }
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

    public static boolean canSpawn(EntityType<TradeNpcEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random){
        return Villager.checkMobSpawnRules(entityType, level, spawnType, position, random);
    }
    public boolean hasLoggedTexture = false;

    public boolean hasLoggedTexture() {
        return hasLoggedTexture;
    }

    public void setLoggedTexture(boolean hasLoggedTexture) {
        this.hasLoggedTexture = hasLoggedTexture;
    }
    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        initializeNpcData();
        LOGGER.info("Entité ajoutée au monde : " + this.getName().getString() + ", texture : " + this.texture);
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



}