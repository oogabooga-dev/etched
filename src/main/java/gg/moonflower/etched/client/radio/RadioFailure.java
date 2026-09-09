package gg.moonflower.etched.client.radio;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * A classified radio failure that can safely be retained by a session.
 */
public record RadioFailure(Code code, boolean recoverable, String message, @Nullable Throwable cause) {

    public RadioFailure {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
    }

    public static RadioFailure recoverable(Code code, String message, @Nullable Throwable cause) {
        return new RadioFailure(code, true, message, cause);
    }

    public static RadioFailure fatal(Code code, String message, @Nullable Throwable cause) {
        return new RadioFailure(code, false, message, cause);
    }

    public enum Code {
        INVALID_URL,
        BLOCKED_ADDRESS,
        CONNECT_TIMEOUT,
        READ_TIMEOUT,
        HTTP_STATUS,
        TOO_MANY_REDIRECTS,
        PLAYLIST_TOO_LARGE,
        UNSUPPORTED_HLS,
        UNSUPPORTED_AAC,
        UNSUPPORTED_AUDIO,
        DECODER_FAILURE,
        RESOURCE_LIMIT,
        UNKNOWN
    }
}
