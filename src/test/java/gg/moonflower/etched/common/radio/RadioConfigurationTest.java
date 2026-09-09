package gg.moonflower.etched.common.radio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioConfigurationTest {

    @Test
    void normalizesMissingUrlWithoutChangingPresentValues() {
        assertEquals("", new RadioConfiguration(null, false).url());
        assertEquals("  https://radio.example/live  ",
                new RadioConfiguration("  https://radio.example/live  ", false).url());
    }

    @Test
    void distinguishesConfigurationFromEnabledPlayback() {
        assertFalse(new RadioConfiguration("", false).isConfigured());
        assertFalse(new RadioConfiguration("", false).isEnabled());
        assertTrue(new RadioConfiguration("https://radio.example/live", false).isConfigured());
        assertTrue(new RadioConfiguration("https://radio.example/live", false).isEnabled());
        assertFalse(new RadioConfiguration("https://radio.example/live", true).isEnabled());
    }
}
