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
        state.receivePlaybackState(false);
        assertTrue(state.loaded());
        assertTrue(state.canPlay());
        assertEquals("https://radio.example/live", state.play().orElseThrow());
        assertTrue(state.canStop());

        state.update("https://radio.example/second");
        assertTrue(state.play().isEmpty());
        assertTrue(state.stop());
        assertFalse(state.canStop());
        state.receivePlaybackState(false);
        assertEquals("https://radio.example/second", state.play().orElseThrow());
        assertTrue(state.canStop());
    }

    @Test
    void invalidOrEmptyInputCannotBePlayedButStartedRadioCanStop() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/live");
        state.receivePlaybackState(false);

        state.update("broken");
        assertFalse(state.valid());
        assertFalse(state.canPlay());
        assertFalse(state.canStop());

        state.update("   ");
        assertTrue(state.valid());
        assertFalse(state.canPlay());
        assertFalse(state.canStop());

        state.receivePlaybackState(true);
        assertTrue(state.canStop());
    }

    @Test
    void stopDoesNotApplyUnsavedUrlEdits() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("");
        state.receivePlaybackState(false);

        state.update("https://radio.example/new");
        assertFalse(state.canStop());
        assertEquals("https://radio.example/new", state.play().orElseThrow());
        assertTrue(state.stop());
        assertFalse(state.canStop());
    }

    @Test
    void ignoresDuplicateInitialPacketsButAllowsEditsAfterPlay() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/first");
        state.receivePlaybackState(false);

        assertFalse(state.receiveInitialUrl("https://radio.example/stale"));
        assertEquals("https://radio.example/first", state.value());
        state.play();
        state.update("https://radio.example/late-edit");
        assertEquals("https://radio.example/late-edit", state.value());
    }

    @Test
    void cannotPlayUntilPlaybackStateArrives() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/live");

        assertFalse(state.canPlay());

        state.receivePlaybackState(false);
        assertTrue(state.canPlay());
    }

    @Test
    void disablesPlayWhileStartingOrStartedAndEnablesItAfterStop() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/live");
        state.receivePlaybackState(false);

        assertTrue(state.play().isPresent());
        assertFalse(state.canPlay());
        assertTrue(state.canStop());

        // A stale stopped update must not unlock Play while the start command is pending.
        state.receivePlaybackState(false);
        assertFalse(state.canPlay());
        assertTrue(state.canStop());

        state.receivePlaybackState(true);
        assertFalse(state.canPlay());
        assertTrue(state.canStop());

        assertTrue(state.stop());
        assertFalse(state.canPlay());
        assertFalse(state.canStop());

        // A stale started update must not unlock Stop while stopping.
        state.receivePlaybackState(true);
        assertFalse(state.canStop());

        state.receivePlaybackState(false);
        assertTrue(state.canPlay());
        assertFalse(state.canStop());
    }
}
