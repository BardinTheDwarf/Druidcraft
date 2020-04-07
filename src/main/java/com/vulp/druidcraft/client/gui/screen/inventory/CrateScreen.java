package com.vulp.druidcraft.client.gui.screen.inventory;

import com.mojang.blaze3d.platform.GlStateManager;
import com.vulp.druidcraft.Druidcraft;
import com.vulp.druidcraft.inventory.container.CrateContainer;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// FIX: For some reason the quad crate is rendering an unneeded bit of the inventory on the bottom of the screen.

@OnlyIn(Dist.CLIENT)
public class CrateScreen extends ContainerScreen<CrateContainer> implements IHasContainer<CrateContainer> {
    private static final ResourceLocation CRATE_GUI_TEXTURE = new ResourceLocation(Druidcraft.MODID, "textures/gui/container/large_crate.png");
    private final int inventoryRows;
    private float currentScroll;
    private boolean isScrolling;

    public CrateScreen(CrateContainer container, PlayerInventory playerInv, ITextComponent text) {
        super(container, playerInv, text);
        this.passEvents = false;
        this.inventoryRows = container.getNumRows();
        this.ySize = 222;
        this.xSize = 194;
    }

    public void render(int p_render_1_, int p_render_2_, float p_render_3_) {
        this.renderBackground();
        super.render(p_render_1_, p_render_2_, p_render_3_);
        this.renderHoveredToolTip(p_render_1_, p_render_2_);
    }

    protected boolean clickOnScrollBar(double p_mouseClicked_1_, double p_mouseClicked_3_) {
        int i = this.guiLeft;
        int j = this.guiTop;
        int k = i + 174;
        int l = j + 18;
        int i1 = k + 14;
        int j1 = l + 198;
        return p_mouseClicked_1_ >= (double)k && p_mouseClicked_3_ >= (double)l && p_mouseClicked_1_ < (double)i1 && p_mouseClicked_3_ < (double)j1;
    }

    public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
        if (p_mouseClicked_5_ == 0 && this.clickOnScrollBar(p_mouseClicked_1_, p_mouseClicked_3_)) {
            this.isScrolling = true;
            return true;
        }
        return super.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
    }

    public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
        int i = (((this.container).getNumRows() * 9) + 9 - 1) / 9 - 6;
        this.currentScroll = (float)((double)this.currentScroll - p_mouseScrolled_5_ / (double)i);
        this.currentScroll = MathHelper.clamp(this.currentScroll, 0.0F, 1.0F);
        this.container.scrollTo(this.currentScroll);
        return true;
    }

    public boolean mouseDragged(double p_mouseDragged_1_, double p_mouseDragged_3_, int p_mouseDragged_5_, double p_mouseDragged_6_, double p_mouseDragged_8_) {
        if (this.isScrolling) {
            int i = this.guiTop + 18;
            int j = i + 198;
            this.currentScroll = ((float)p_mouseDragged_3_ - (float)i - 7.5F) / ((float)(j - i) - 15.0F);
            this.currentScroll = MathHelper.clamp(this.currentScroll, 0.0F, 1.0F);
            this.container.scrollTo(this.currentScroll);
            return true;
        } else {
            return super.mouseDragged(p_mouseDragged_1_, p_mouseDragged_3_, p_mouseDragged_5_, p_mouseDragged_6_, p_mouseDragged_8_);
        }
    }

    public boolean mouseReleased(double p_mouseReleased_1_, double p_mouseReleased_3_, int p_mouseReleased_5_) {
        if (p_mouseReleased_5_ == 0) {
            this.isScrolling = false;
        }

        return super.mouseReleased(p_mouseReleased_1_, p_mouseReleased_3_, p_mouseReleased_5_);
    }

    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F, 4210752);
        this.font.drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F, (float)(this.ySize - 96 + 2), 4210752);
    }

    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(CRATE_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.blit(i, j, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
        this.blit(i, j + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);

        this.blit(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        int x = this.guiLeft + 174;
        int y = this.guiTop + 18;
        this.blit(x, y + (int)(181 * this.currentScroll), 232, 0, 12, 15);
    }
}