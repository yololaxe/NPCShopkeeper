package fr.renblood.npcshopkeeper.client.gui;
import fr.renblood.npcshopkeeper.world.inventory.CategoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.List;
import fr.renblood.npcshopkeeper.data.io.JsonFileManager;
import net.minecraft.world.entity.player.Inventory;

public class CategoryScreen extends AbstractContainerScreen<CategoryMenu> {

    private final List<String> categories; // This will hold the list of trade categories

    public CategoryScreen(CategoryMenu container, Inventory inventory, Component text) {
        super(container, inventory, text);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.categories = menu.getCategories(); // Get category names from your manager
    }

    @Override
    protected void init() {
        super.init();
        int y = this.height / 2 - 40;

        // Dynamically add buttons for each category
        for (int i = 0; i < categories.size(); i++) {
            String category = categories.get(i);
            this.addRenderableWidget(
                    Button.builder(Component.literal(category), button -> {
                                // Ouvrir une nouvelle interface pour cette catégorie
                                Minecraft.getInstance().setScreen(new SeeTradesScreen(0, category));
                            })
                            .bounds(this.width / 2 - 100, y + i * 25, 200, 20) // Position et dimensions
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
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
