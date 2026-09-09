package gg.moonflower.etched.client.radio.net;

import java.net.URI;
import java.util.Objects;

/**
 * A GET request whose headers are controlled by the radio transport.
 */
public record RadioHttpRequest(URI uri, Purpose purpose) {

    public RadioHttpRequest {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(purpose, "purpose");
    }

    public static RadioHttpRequest resource(URI uri) {
        return new RadioHttpRequest(uri, Purpose.RESOURCE);
    }

    public static RadioHttpRequest audio(URI uri) {
        return new RadioHttpRequest(uri, Purpose.AUDIO);
    }

    public enum Purpose {
        RESOURCE,
        AUDIO
    }
}
