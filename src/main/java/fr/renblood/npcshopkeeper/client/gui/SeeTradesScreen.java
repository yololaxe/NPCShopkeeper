package fr.renblood.npcshopkeeper.client.gui;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import java.util.List;
import fr.renblood.npcshopkeeper.data.Trade;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;
public class SeeTradesScreen extends AbstractContainerScreen<TradeListMenu> {

    private final String category;
    private final List<Trade> trades; // This holds trades for the selected category

    public SeeTradesScreen(String category) {
        super(new TradeListMenu(), Minecraft.getInstance().player.getInventory(), Component.literal(category));
        this.category = category;
        this.trades = JsonTradeFileManager.getTradesForCategory(category); // Fetch trades for this category
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
            this.addRenderableWidget(new Button(this.width / 2 - 100, y + i * 25, 200, 20, Component.literal(trade.getName()), button -> {
                // When a trade is selected, you can display details or start the trade
                Minecraft.getInstance().player.sendMessage(Component.literal("Trade selected: " + trade.getName()));
            }));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
