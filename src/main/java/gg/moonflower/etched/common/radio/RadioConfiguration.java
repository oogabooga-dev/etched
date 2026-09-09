package gg.moonflower.etched.common.radio;

/**
 * Immutable radio state synchronized by the block entity.
 */
public record RadioConfiguration(String url, boolean powered) {

    public RadioConfiguration {
        if (url == null) {
            url = "";
        }
    }

    public boolean isConfigured() {
        return !this.url.isEmpty();
    }

    public boolean isEnabled() {
        return this.isConfigured() && !this.powered;
    }
}
