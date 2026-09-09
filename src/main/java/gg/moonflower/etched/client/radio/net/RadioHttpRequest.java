package gg.moonflower.etched.client.radio.net;

import java.net.URI;
import java.util.Objects;

/**
 * A GET request whose headers are controlled by the radio transport.
 */
public record RadioHttpRequest(URI uri, Purpose purpose, int maxRedirects) {

    public RadioHttpRequest(URI uri, Purpose purpose) {
        this(uri, purpose, Integer.MAX_VALUE);
    }

    public RadioHttpRequest {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(purpose, "purpose");
        if (maxRedirects < 0) {
            throw new IllegalArgumentException("The redirect limit cannot be negative");
        }
    }

    public static RadioHttpRequest resource(URI uri) {
        return new RadioHttpRequest(uri, Purpose.RESOURCE, Integer.MAX_VALUE);
    }

    public static RadioHttpRequest audio(URI uri) {
        return new RadioHttpRequest(uri, Purpose.AUDIO, Integer.MAX_VALUE);
    }

    public RadioHttpRequest withMaxRedirects(int maxRedirects) {
        return new RadioHttpRequest(this.uri, this.purpose, maxRedirects);
    }

    public enum Purpose {
        RESOURCE,
        AUDIO
    }
}
