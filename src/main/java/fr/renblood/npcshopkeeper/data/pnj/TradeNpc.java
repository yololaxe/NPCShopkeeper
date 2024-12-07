package fr.renblood.npcshopkeeper.data.pnj;

import fr.renblood.npcshopkeeper.entity.TradeNpcEntity;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

public class TradeNpc {
    private String npcId;
    private String npcName;
    private ArrayList<String> texts;
    private String texture;
    private BlockPos position;
    private TradeNpcEntity entity;

    public TradeNpc(String npcId, String npcName, ArrayList<String> texts, String texture, BlockPos position, TradeNpcEntity entity) {
        this.npcId = npcId;
        this.npcName = npcName;
        this.texts = texts;
        this.texture = texture;
        this.position = position;
        this.entity = entity;
    }

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

    public TradeNpcEntity getEntity() {
        return entity;
    }

    public void setEntity(TradeNpcEntity entity) {
        this.entity = entity;
    }
}