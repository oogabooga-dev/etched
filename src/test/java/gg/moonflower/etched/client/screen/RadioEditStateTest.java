package gg.moonflower.etched.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioEditStateTest {

    @Test
    void cannotControlRadioBeforeInitialUrlArrives() {
        RadioEditState state = new RadioEditState();

        assertFalse(state.loaded());
        assertFalse(state.canPlay());
        assertFalse(state.canStop());
        assertTrue(state.play().isEmpty());
        assertFalse(state.stop());
    }

    @Test
    void playsNormalizedUrlsWithoutClosingTheEditor() {
        RadioEditState state = new RadioEditState();

        assertTrue(state.receiveInitialUrl("  https://radio.example/live  "));
        assertTrue(state.loaded());
        assertTrue(state.canPlay());
        assertEquals("https://radio.example/live", state.play().orElseThrow());

        state.update("https://radio.example/second");
        assertEquals("https://radio.example/second", state.play().orElseThrow());
        assertTrue(state.canStop());
    }

    @Test
    void invalidOrEmptyInputCannotBePlayedButConfiguredRadioCanStop() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/live");

        state.update("broken");
        assertFalse(state.valid());
        assertFalse(state.canPlay());
        assertTrue(state.stop());

        state.update("   ");
        assertTrue(state.valid());
        assertFalse(state.canPlay());
        assertTrue(state.stop());
    }

    @Test
    void stopDoesNotApplyUnsavedUrlEdits() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("");

        state.update("https://radio.example/new");
        assertFalse(state.canStop());
        assertEquals("https://radio.example/new", state.play().orElseThrow());
        assertTrue(state.stop());
    }

    @Test
    void ignoresDuplicateInitialPacketsButAllowsEditsAfterPlay() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/first");

        assertFalse(state.receiveInitialUrl("https://radio.example/stale"));
        assertEquals("https://radio.example/first", state.value());
        state.play();
        state.update("https://radio.example/late-edit");
        assertEquals("https://radio.example/late-edit", state.value());
    }
}
