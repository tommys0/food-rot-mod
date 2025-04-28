package jabkari.foodrotmod.client;

import jabkari.foodrotmod.Foodrotmod; // Only needed if using MOD_ID for texture path
import jabkari.foodrotmod.menu.IceBoxMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class IceBoxScreen extends AbstractContainerScreen<IceBoxMenu> {

    // Use vanilla single chest background texture
    private static final ResourceLocation GUI_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public IceBoxScreen(IceBoxMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // Standard chest GUI height (background texture size)
        this.imageHeight = 114 + 3 * 18; // Top part + 3 rows of inventory
        // Player inventory label Y position needs adjustment for this height
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Center title label if needed (usually defaults are fine)
        // this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        // Draw the background texture
        guiGraphics.blit(GUI_TEXTURE, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta); // Dim background
        super.render(guiGraphics, mouseX, mouseY, delta); // Render GUI elements (bg, slots)
        this.renderTooltip(guiGraphics, mouseX, mouseY); // Render tooltips on hover
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw the container title ("Ice Box")
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false); // Dark gray color
        // Draw the player inventory title ("Inventory")
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }
}