package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class M3uParserTest {

    @Test
    void parsesAbsoluteEntriesFromExtendedPlaylist() throws Exception {
        String playlist = """
                #EXTM3U
                #EXTINF:-1,Primary station
                https://radio.example/primary
                # A comment between entries
                https://radio.example/fallback
                """;

        List<URL> entries = M3uParser.parse(new InputStreamReader(
                new ByteArrayInputStream(playlist.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8));

        assertEquals(List.of(
                URI.create("https://radio.example/primary").toURL(),
                URI.create("https://radio.example/fallback").toURL()), entries);
    }
}
