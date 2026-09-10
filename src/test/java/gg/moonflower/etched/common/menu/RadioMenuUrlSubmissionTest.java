package gg.moonflower.etched.common.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioMenuUrlSubmissionTest {

    @Test
    void acceptsOnlyOneValidSubmission() {
        RadioMenu.UrlSubmission submission = new RadioMenu.UrlSubmission();
        List<String> accepted = new ArrayList<>();

        assertFalse(submission.submit("not a url", accepted::add));
        assertTrue(submission.submit("  https://radio.example/live?token=A%2BB  ", accepted::add));
        assertFalse(submission.submit("https://radio.example/second", accepted::add));

        assertEquals(List.of("https://radio.example/live?token=A%2BB"), accepted);
    }

    @Test
    void intentionalEmptySubmissionClearsOnlyOnce() {
        RadioMenu.UrlSubmission submission = new RadioMenu.UrlSubmission();
        List<String> accepted = new ArrayList<>();

        assertTrue(submission.submit("   ", accepted::add));
        assertFalse(submission.submit("https://radio.example/live", accepted::add));

        assertEquals(List.of(""), accepted);
    }
}
