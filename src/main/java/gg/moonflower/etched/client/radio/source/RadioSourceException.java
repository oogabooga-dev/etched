package gg.moonflower.etched.client.radio.source;

import gg.moonflower.etched.client.radio.RadioFailure;
import gg.moonflower.etched.client.radio.net.RadioTransportException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public final class RadioSourceException extends IOException {

    private final RadioFailure.Code code;
    private final boolean recoverable;
    private final long retryAfterMillis;

    public RadioSourceException(RadioFailure.Code code, boolean recoverable, String message,
                                 @Nullable Throwable cause) {
        this(code, recoverable, message, cause, RadioFailure.NO_RETRY_AFTER);
    }

    public RadioSourceException(RadioFailure.Code code, boolean recoverable, String message,
                                @Nullable Throwable cause, long retryAfterMillis) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.recoverable = recoverable;
        if (retryAfterMillis < RadioFailure.NO_RETRY_AFTER) {
            throw new IllegalArgumentException("Retry-After must be non-negative or absent");
        }
        this.retryAfterMillis = retryAfterMillis;
    }

    public RadioFailure.Code code() {
        return this.code;
    }

    public boolean recoverable() {
        return this.recoverable;
    }

    public long retryAfterMillis() {
        return this.retryAfterMillis;
    }

    public RadioFailure toFailure() {
        return new RadioFailure(this.code, this.recoverable, this.getMessage(), this, this.retryAfterMillis);
    }

    static RadioSourceException fromTransport(RadioTransportException exception) {
        return new RadioSourceException(exception.code(), exception.recoverable(),
                exception.getMessage(), exception);
    }
}
