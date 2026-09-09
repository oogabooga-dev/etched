package gg.moonflower.etched.client.radio.net;

import gg.moonflower.etched.client.radio.RadioFailure;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

/**
 * A classified HTTP or network-policy failure.
 */
public final class RadioTransportException extends IOException {

    private final RadioFailure.Code code;
    private final boolean recoverable;

    public RadioTransportException(RadioFailure.Code code, boolean recoverable, String message,
                                   @Nullable Throwable cause) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.recoverable = recoverable;
    }

    public RadioFailure.Code code() {
        return this.code;
    }

    public boolean recoverable() {
        return this.recoverable;
    }

    public RadioFailure toFailure() {
        return new RadioFailure(this.code, this.recoverable, this.getMessage(), this);
    }
}
