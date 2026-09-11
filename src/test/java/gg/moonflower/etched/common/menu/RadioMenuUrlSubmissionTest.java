package gg.moonflower.etched.common.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioMenuUrlSubmissionTest {

    @Test
    void acceptsRepeatedValidControlCommands() {
        RadioMenu.UrlSubmission submission = new RadioMenu.UrlSubmission();
        List<String> accepted = new ArrayList<>();

        assertFalse(submission.submit("not a url", accepted::add));
        assertTrue(submission.submit("  https://radio.example/live?token=A%2BB  ", accepted::add));
        assertTrue(submission.submit("   ", accepted::add));
        assertTrue(submission.submit("https://radio.example/second", accepted::add));

        assertEquals(List.of("https://radio.example/live?token=A%2BB", "", "https://radio.example/second"), accepted);
    }

    @Test
    void boundsCommandsAcceptedByOneOpenMenu() {
        RadioMenu.UrlSubmission submission = new RadioMenu.UrlSubmission();
        List<String> accepted = new ArrayList<>();

        for (int i = 0; i < RadioMenu.UrlSubmission.MAX_COMMANDS; i++) {
            assertTrue(submission.submit("https://radio.example/" + i, accepted::add));
        }
        assertFalse(submission.submit("https://radio.example/excess", accepted::add));
        assertTrue(submission.submit("", accepted::add));

        assertEquals(RadioMenu.UrlSubmission.MAX_COMMANDS + 1, accepted.size());
        assertEquals("", accepted.get(accepted.size() - 1));
    }
}
