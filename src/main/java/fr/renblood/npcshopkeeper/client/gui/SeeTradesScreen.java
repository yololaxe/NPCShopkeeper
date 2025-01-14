package fr.renblood.npcshopkeeper.client.gui;
import fr.renblood.npcshopkeeper.world.inventory.TradeListMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.List;
import fr.renblood.npcshopkeeper.data.Trade.Trade;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
public class SeeTradesScreen extends AbstractContainerScreen<TradeListMenu> {

    private final String category;
    private final List<Trade> trades; // This holds trades for the selected category

    public SeeTradesScreen(int id, String category) {
        super(new TradeListMenu(id, Minecraft.getInstance().player.getInventory()),
                Minecraft.getInstance().player.getInventory(),
                Component.literal(category));
        this.category = category;
        this.trades = JsonTradeFileManager.getTradesByCategory(category); // Récupérer les trades
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 2 - 40;

        // Dynamically add buttons for each trade in the category
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            this.addRenderableWidget(Button.builder(
                            Component.literal(trade.getName()), // Texte affiché sur le bouton
                            button -> {
                                // Action à effectuer lors du clic sur le bouton
                                Minecraft.getInstance().player.displayClientMessage(
                                        Component.literal("Trade selected: " + trade.getName()), true);
                            })
                    .bounds(this.width / 2 - 100, y + i * 25, 200, 20) // Position et dimensions du bouton
                    .build()
            );
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        return;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
