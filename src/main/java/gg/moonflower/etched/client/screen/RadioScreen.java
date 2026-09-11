package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.menu.RadioMenu;
import gg.moonflower.etched.common.network.EtchedMessages;
import gg.moonflower.etched.common.network.play.ServerboundSetUrlPacket;
import gg.moonflower.etched.common.radio.RadioUrlValidator;
import gg.moonflower.etched.core.Etched;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * @author Ocelot
 */
public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Etched.MOD_ID, "textures/gui/radio.png");
    private static final Component LOADING_URL = Component.translatable("screen." + Etched.MOD_ID + ".radio.loading_url");
    private static final Component INVALID_URL = Component.translatable("screen." + Etched.MOD_ID + ".radio.error.invalid_url");
    private static final Component PLAY = Component.translatable("screen." + Etched.MOD_ID + ".radio.play");
    private static final Component STOP = Component.translatable("screen." + Etched.MOD_ID + ".radio.stop");
    private static final Component CLOSE = Component.translatable("screen." + Etched.MOD_ID + ".radio.close");
    private static final int BACKGROUND_HEIGHT = 39;
    private static final int BUTTON_WIDTH = 56;
    private static final int BUTTON_GAP = 4;

    private final RadioEditState editState = new RadioEditState();
    private EditBox url;
    private Button playButton;
    private Button stopButton;

    public RadioScreen(RadioMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.imageHeight = 51;
    }

    @Override
    protected void init() {
        super.init();
        this.url = new EditBox(this.font, this.leftPos + 10, this.topPos + 21, 154, 16,
                Component.translatable("container." + Etched.MOD_ID + ".radio.url"));
        this.url.setTextColor(-1);
        this.url.setTextColorUneditable(-1);
        this.url.setBordered(false);
        this.url.setMaxLength(RadioUrlValidator.MAX_LENGTH);
        this.url.setVisible(this.editState.loaded());
        this.url.setEditable(this.editState.loaded());
        this.url.setCanLoseFocus(false);
        this.url.setValue(this.editState.value());
        this.url.setResponder(value -> {
            this.editState.update(value);
            this.updateActionButtons();
        });
        this.addRenderableWidget(this.url);
        int buttonY = this.topPos + this.imageHeight + 5;
        this.playButton = this.addRenderableWidget(Button.builder(PLAY, button ->
                        this.editState.play().ifPresent(this::sendUrl))
                .bounds(this.leftPos, buttonY, BUTTON_WIDTH, 20)
                .build());
        this.stopButton = this.addRenderableWidget(Button.builder(STOP, button -> {
                    if (this.editState.stop()) {
                        this.sendUrl("");
                    }
                })
                .bounds(this.leftPos + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, 20)
                .build());
        this.addRenderableWidget(Button.builder(CLOSE, button -> this.onClose())
                .bounds(this.leftPos + (BUTTON_WIDTH + BUTTON_GAP) * 2, buttonY, BUTTON_WIDTH, 20)
                .build());
        this.updateActionButtons();
    }

    @Override
    public void containerTick() {
        this.url.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, BACKGROUND_HEIGHT);
        guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 18, 0, this.editState.loaded() ? 39 : 53, 160, 14);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        if (!this.editState.loaded()) {
            guiGraphics.drawString(this.font, LOADING_URL, 8, 41, 8421504, false);
        } else if (!this.editState.valid()) {
            guiGraphics.drawString(this.font, INVALID_URL, 8, 41, 0xFF5555, false);
        }
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        return this.url.keyPressed(i, j, k) || (this.url.isFocused() && this.url.isVisible() && i != 256) || super.keyPressed(i, j, k);
    }

    public void receiveUrl(String url) {
        if (!this.editState.receiveInitialUrl(url)) {
            return;
        }
        this.url.setVisible(true);
        this.url.setEditable(true);
        this.url.setValue(this.editState.value());
        this.setFocused(this.url);
        this.url.setFocused(true);
        this.updateActionButtons();
    }

    private void sendUrl(String value) {
        EtchedMessages.PLAY.sendToServer(new ServerboundSetUrlPacket(value));
        this.updateActionButtons();
    }

    private void updateActionButtons() {
        if (this.playButton != null) {
            this.playButton.active = this.editState.canPlay();
        }
        if (this.stopButton != null) {
            this.stopButton.active = this.editState.canStop();
        }
    }
}
