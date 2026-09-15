package gg.moonflower.etched.common.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

/** Stores a radio station separately from whether manual playback is enabled. */
final class RadioControlState {

    private static final String ACTIVE_URL_TAG = "Url";
    private static final String STORED_URL_TAG = "StoredUrl";

    private String storedUrl;
    private boolean enabled;

    void load(CompoundTag nbt) {
        String activeUrl = readUrl(nbt, ACTIVE_URL_TAG);
        if (nbt.contains(STORED_URL_TAG, Tag.TAG_STRING)) {
            this.storedUrl = normalize(nbt.getString(STORED_URL_TAG));
        } else if (activeUrl != null || !nbt.contains(ACTIVE_URL_TAG, Tag.TAG_STRING)) {
            this.storedUrl = activeUrl;
        }
        this.enabled = this.storedUrl != null && activeUrl != null;
    }

    void save(CompoundTag nbt) {
        if (this.storedUrl == null) {
            return;
        }

        nbt.putString(STORED_URL_TAG, this.storedUrl);
        nbt.putString(ACTIVE_URL_TAG, this.enabled ? this.storedUrl : "");
    }

    boolean apply(String url) {
        String normalized = normalize(url);
        if (normalized == null) {
            if (!this.enabled) {
                return false;
            }
            this.enabled = false;
            return true;
        }

        if (this.enabled && Objects.equals(this.storedUrl, normalized)) {
            return false;
        }
        this.storedUrl = normalized;
        this.enabled = true;
        return true;
    }

    boolean clear() {
        if (this.storedUrl == null && !this.enabled) {
            return false;
        }
        this.storedUrl = null;
        this.enabled = false;
        return true;
    }

    String storedUrl() {
        return this.storedUrl;
    }

    String activeUrl() {
        return this.enabled ? this.storedUrl : null;
    }

    boolean enabled() {
        return this.enabled;
    }

    private static String readUrl(CompoundTag nbt, String key) {
        return nbt.contains(key, Tag.TAG_STRING) ? normalize(nbt.getString(key)) : null;
    }

    private static String normalize(String url) {
        return url == null || url.isBlank() ? null : url;
    }
}
