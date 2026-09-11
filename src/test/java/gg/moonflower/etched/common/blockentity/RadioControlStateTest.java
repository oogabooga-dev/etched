package gg.moonflower.etched.common.blockentity;

import gg.moonflower.etched.common.radio.RadioConfiguration;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioControlStateTest {

    @Test
    void loadsLegacyUrlAsStoredAndEnabled() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("Url", "https://radio.example/live");
        RadioControlState state = new RadioControlState();

        state.load(legacy);

        assertEquals("https://radio.example/live", state.storedUrl());
        assertEquals("https://radio.example/live", state.activeUrl());
        assertTrue(state.enabled());
    }

    @Test
    void stoppedStationRoundTripsWhileLegacyUrlStaysEmpty() {
        RadioControlState state = new RadioControlState();
        assertTrue(state.apply("https://radio.example/live"));
        assertTrue(state.apply(""));
        CompoundTag saved = new CompoundTag();

        state.save(saved);

        assertEquals("", saved.getString("Url"));
        assertEquals("https://radio.example/live", saved.getString("StoredUrl"));

        RadioControlState loaded = new RadioControlState();
        loaded.load(saved);
        assertEquals("https://radio.example/live", loaded.storedUrl());
        assertNull(loaded.activeUrl());
        assertFalse(loaded.enabled());
    }

    @Test
    void enabledStationKeepsTheLegacyUrlVisible() {
        RadioControlState state = new RadioControlState();
        state.apply("https://radio.example/live");
        CompoundTag saved = new CompoundTag();

        state.save(saved);

        assertEquals("https://radio.example/live", saved.getString("Url"));
        assertEquals("https://radio.example/live", saved.getString("StoredUrl"));
    }

    @Test
    void playResumesStoredStationAndCanReplaceIt() {
        RadioControlState state = new RadioControlState();
        state.apply("https://radio.example/first");
        state.apply("");

        assertTrue(state.apply("https://radio.example/first"));
        assertFalse(state.apply("https://radio.example/first"));
        assertEquals("https://radio.example/first", state.activeUrl());
        assertTrue(state.apply("https://radio.example/second"));
        assertEquals("https://radio.example/second", state.activeUrl());
    }

    @Test
    void clearRemovesBothActiveAndStoredState() {
        RadioControlState state = new RadioControlState();
        state.apply("https://radio.example/live");

        assertTrue(state.clear());
        assertFalse(state.clear());
        assertNull(state.storedUrl());
        assertNull(state.activeUrl());

        CompoundTag saved = new CompoundTag();
        state.save(saved);
        assertFalse(saved.contains("Url"));
        assertFalse(saved.contains("StoredUrl"));
    }

    @Test
    void manualStopRemainsDisabledAcrossRedstoneChanges() {
        RadioControlState state = new RadioControlState();
        state.apply("https://radio.example/live");
        state.apply("");

        assertFalse(new RadioConfiguration(state.activeUrl(), false).isEnabled());
        assertFalse(new RadioConfiguration(state.activeUrl(), true).isEnabled());

        state.apply("https://radio.example/live");
        assertFalse(new RadioConfiguration(state.activeUrl(), true).isEnabled());
        assertTrue(new RadioConfiguration(state.activeUrl(), false).isEnabled());
    }
}
