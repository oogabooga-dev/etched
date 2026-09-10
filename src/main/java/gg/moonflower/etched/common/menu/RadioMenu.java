package gg.moonflower.etched.common.menu;

import gg.moonflower.etched.common.radio.RadioUrlValidator;
import gg.moonflower.etched.core.registry.EtchedBlocks;
import gg.moonflower.etched.core.registry.EtchedMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * @author Ocelot
 */
public class RadioMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    // Workaround for thread concurrency issues
    private final Consumer<String> urlConsumer;
    private final UrlSubmission submission = new UrlSubmission();

    public RadioMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL, url -> {
        });
    }

    public RadioMenu(int id, Inventory inventory, ContainerLevelAccess access, Consumer<String> containerLevelAccess) {
        super(EtchedMenus.RADIO_MENU.get(), id);
        this.access = access;
        this.urlConsumer = containerLevelAccess;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EtchedBlocks.RADIO.get());
    }

    /**
     * Sets the URL for the resulting stack to the specified value.
     *
     * @param url The new URL
     */
    public void setUrl(String url) {
        this.submitUrl(url);
    }

    /**
     * Applies the first valid URL submitted for this menu instance.
     *
     * @return Whether this submission was accepted
     */
    public boolean submitUrl(String url) {
        return this.submission.submit(url, this.urlConsumer);
    }

    static final class UrlSubmission {

        private boolean submitted;

        boolean submit(String url, Consumer<String> consumer) {
            RadioUrlValidator.Result validation = RadioUrlValidator.validate(url);
            if (this.submitted || !validation.valid()) {
                return false;
            }
            this.submitted = true;
            consumer.accept(validation.normalized());
            return true;
        }
    }
}
