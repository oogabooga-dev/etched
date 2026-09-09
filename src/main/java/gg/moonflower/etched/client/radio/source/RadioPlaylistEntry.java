package gg.moonflower.etched.client.radio.source;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

record RadioPlaylistEntry(URI uri, @Nullable String title) {

    RadioPlaylistEntry {
        Objects.requireNonNull(uri, "uri");
    }
}
