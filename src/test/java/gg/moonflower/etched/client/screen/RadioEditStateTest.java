package gg.moonflower.etched.client.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioEditStateTest {

    @Test
    void cannotSubmitBeforeInitialUrlArrives() {
        RadioEditState state = new RadioEditState();

        assertFalse(state.loaded());
        assertFalse(state.canSubmit());
        assertTrue(state.submit().isEmpty());
    }

    @Test
    void enablesValidInitialUrlAndSubmitsItsNormalizedValueOnce() {
        RadioEditState state = new RadioEditState();

        assertTrue(state.receiveInitialUrl("  https://radio.example/live  "));
        assertTrue(state.loaded());
        assertTrue(state.canSubmit());
        assertEquals("https://radio.example/live", state.submit().orElseThrow());
        assertFalse(state.canSubmit());
        assertTrue(state.submit().isEmpty());
    }

    @Test
    void invalidInputCanBeCorrectedOrCleared() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/live");

        state.update("broken");
        assertFalse(state.valid());
        assertFalse(state.canSubmit());

        state.update("   ");
        assertTrue(state.valid());
        assertEquals("", state.submit().orElseThrow());
    }

    @Test
    void ignoresDuplicateInitialPacketsAndEditsAfterSubmission() {
        RadioEditState state = new RadioEditState();
        state.receiveInitialUrl("https://radio.example/first");

        assertFalse(state.receiveInitialUrl("https://radio.example/stale"));
        assertEquals("https://radio.example/first", state.value());
        state.submit();
        state.update("https://radio.example/late-edit");
        assertEquals("https://radio.example/first", state.value());
    }
}
