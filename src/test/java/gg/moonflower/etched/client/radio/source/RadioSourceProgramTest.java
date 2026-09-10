package gg.moonflower.etched.client.radio.source;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadioSourceProgramTest {

    private static final URI SOURCE = URI.create("https://service.example/album");

    @Test
    void stationRequiresExactlyOneTrack() {
        assertThrows(IllegalArgumentException.class,
                () -> new RadioSourceProgram(RadioSourceProgram.Kind.STATION, SOURCE, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new RadioSourceProgram(RadioSourceProgram.Kind.STATION, SOURCE,
                        List.of(track("one"), track("two"))));

        RadioSourceProgram station = new RadioSourceProgram(
                RadioSourceProgram.Kind.STATION, SOURCE, List.of(track("one")));

        assertEquals(RadioSourceProgram.Kind.STATION, station.kind());
        assertEquals(1, station.tracks().size());
    }

    @Test
    void serviceRequiresTracksAndPreservesTheirOrderAndMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new RadioSourceProgram(RadioSourceProgram.Kind.SERVICE_TRACKS, SOURCE, List.of()));
        List<RadioSourceProgram.Track> input = new ArrayList<>(List.of(track("one"), track("two")));

        RadioSourceProgram service = new RadioSourceProgram(
                RadioSourceProgram.Kind.SERVICE_TRACKS, SOURCE, input);
        input.clear();

        assertEquals(RadioSourceProgram.Kind.SERVICE_TRACKS, service.kind());
        assertEquals(SOURCE, service.source());
        assertEquals(List.of("one", "two"), service.tracks().stream()
                .map(RadioSourceProgram.Track::title)
                .toList());
        assertThrows(UnsupportedOperationException.class, () -> service.tracks().clear());
    }

    private static RadioSourceProgram.Track track(String title) {
        return new RadioSourceProgram.Track(
                URI.create("https://media.example/" + title + ".mp3"), title,
                context -> {
                    throw new AssertionError("Track should not be opened by this test");
                });
    }
}
