package fr.renblood.npcshopkeeper.client.gui;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import java.util.List;
import fr.renblood.npcshopkeeper.manager.JsonTradeFileManager;

public class CategoryScreen extends AbstractContainerScreen<CategoryMenu> {

    private final List<String> categories; // This will hold the list of trade categories

    public CategoryScreen(CategoryMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.categories = JsonTradeFileManager.readCategoryNames(); // Get category names from your manager
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 2 - 40;

        // Dynamically add buttons for each category
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            this.addRenderableWidget(new Button(this.width / 2 - 100, y + i * 25, 200, 20, Component.literal(category), button -> {
                // On button click, open a new screen showing the trades in this category
                Minecraft.getInstance().setScreen(new TradeListScreen(category));
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
