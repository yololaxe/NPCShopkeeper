package fr.renblood.npcshopkeeper.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.icu.impl.Pair;
import fr.renblood.npcshopkeeper.data.Trade;
import fr.renblood.npcshopkeeper.data.TradeHistory;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
import fr.renblood.npcshopkeeper.procedures.TradeCommandProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fr.renblood.npcshopkeeper.manager.ConstantManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

;

public class TradeNpcEntity extends Villager {

    private String npcId;
    private String npcName;
    private ArrayList<String> texts;
    private String texture;
    private BlockPos position;
    private String tradeCategory; // Catégorie du trade assignée à ce PNJ
    private Trade trade;
    private TradeHistory tradeHistory;
    private static final Logger LOGGER = LogManager.getLogger(TradeCommandProcedure.class);

    public TradeNpcEntity(EntityType<TradeNpcEntity> type, Level world) {
        super(type, world);
        this.registerGoals();
        this.initializeNpcData(); // Initialiser les données PNJ
    }

    private void initializeNpcData() {
        try {
            JsonArray pnjs = ConstantManager.getPnjData(); // Récupère les PNJs depuis ConstantManager
            if (pnjs != null && pnjs.size() > 0) {
                // Choisir un PNJ aléatoire
                JsonObject randomPnj = pnjs.get(this.random.nextInt(pnjs.size())).getAsJsonObject();

                this.npcId = randomPnj.has("name") ? randomPnj.get("name").getAsString() : "Unknown";
                this.npcName = this.npcId;
                this.texture = randomPnj.has("texture") ? randomPnj.get("texture").getAsString() : "textures/entity/default.png";
                this.texts = new ArrayList<>();
                if (randomPnj.has("texts")) {
                    randomPnj.getAsJsonArray("texts").forEach(text -> texts.add(text.getAsString()));
                }
            } else {
                LOGGER.warn("Aucun PNJ défini dans les données JSON !");
                this.npcId = "Default";
                this.npcName = "Default";
                this.texture = "textures/entity/default.png";
                this.texts = new ArrayList<>();
                this.texts.add("Salut !");
                this.texts.add("Je suis un PNJ générique.");
            }

            this.setCustomName(Component.literal(this.npcName));
            LOGGER.info("PNJ initialisé avec succès : " + this.npcName);

        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'initialisation du PNJ : " + e.getMessage(), e);
            this.npcId = "Error";
            this.npcName = "Error";
            this.texture = "textures/entity/error.png";
            this.texts = new ArrayList<>();
            this.texts.add("Erreur lors du chargement des données.");
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
        if (!this.level().isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                try {
                    // Vérifier si un trade existe déjà pour ce PNJ
                    Pair<Boolean, TradeHistory> tradeStatus = JsonTradeFileManager.checkTradeStatusForNpc(this.npcId);
                    position = player.blockPosition();

                    if (tradeStatus.first) {
                        // Si un trade existe, récupérer ses détails
                        tradeHistory = tradeStatus.second;
                        if (tradeHistory == null || tradeHistory.getTradeName() == null || tradeHistory.getTradeItems().isEmpty()) {
                            player.displayClientMessage(Component.literal("Erreur : données de trade manquantes pour le PNJ."), true);
                            LOGGER.error("Données de trade manquantes pour le PNJ : " + this.npcId);
                            return InteractionResult.FAIL;
                        }

                        // Utiliser le trade existant
                        String existingTradeName = tradeHistory.getTradeName();
                        player.displayClientMessage(Component.literal("Un trade existant a été trouvé : " + existingTradeName), true);


                        TradeCommandProcedure.execute(
                                this.level(), position.getX(), position.getY(), position.getZ(),
                                existingTradeName, player, this.npcId, this.npcName
                        );
                    } else {
                        // Sinon, créer un nouveau trade aléatoire
                        if (this.tradeCategory != null) {
                            List<Trade> trades = JsonTradeFileManager.getTradesByCategory(this.tradeCategory);

                            if (!trades.isEmpty()) {
                                trade = trades.get(new Random().nextInt(trades.size()));
                                player.displayClientMessage(Component.literal("Nouveau trade généré : " + trade.getName()), true);


                                TradeCommandProcedure.execute(
                                        this.level(), position.getX(), position.getY(), position.getZ(),
                                        trade.getName(), player, this.npcId, this.npcName
                                );
                            } else {
                                player.displayClientMessage(Component.literal("Aucun trade trouvé dans cette catégorie : " + tradeCategory), true);
                            }
                        } else {
                            player.displayClientMessage(Component.literal("Ce PNJ n'a pas de catégorie de trade assignée."), true);
                        }
                    }
                } catch (Exception e) {
                    player.displayClientMessage(Component.literal("Erreur lors de l'exécution de TradeCommandProcedure : " + e.getMessage()), true);
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
                .add(Attributes.ATTACK_DAMAGE, 0.0D)  // Désactiver les attaques
                .add(Attributes.FOLLOW_RANGE, 0.0D);  // Désactiver la portée de suivi
    }

    public static boolean canSpawn(EntityType<TradeNpcEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos position, RandomSource random){
        return Villager.checkMobSpawnRules(entityType, level, spawnType, position, random);
    }
}