package fr.renblood.npcshopkeeper.data;

public class TradeHistory {
    private String player;
    private String tradeName;
    private boolean isFinished;

    // Constructeur
    public TradeHistory(String player, String tradeName, boolean isFinished) {
        this.player = player;
        this.tradeName = tradeName;
        this.isFinished = isFinished;
    }

    // Getters et Setters
    public String getPlayer() {
        return player;
    }

    public void setPlayer(String player) {
        this.player = player;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    @Override
    public String toString() {
        return "TradeHistory{" +
                "player='" + player + '\'' +
                ", tradeName='" + tradeName + '\'' +
                ", isFinished=" + isFinished +
                '}';
    }
}
