package gg.moonflower.etched.client.radio.source;

import gg.moonflower.etched.client.radio.RadioFailure;
import gg.moonflower.etched.client.radio.net.RadioTransportException;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;

public final class RadioSourceException extends IOException {

    private final RadioFailure.Code code;
    private final boolean recoverable;

    public RadioSourceException(RadioFailure.Code code, boolean recoverable, String message,
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

    static RadioSourceException fromTransport(RadioTransportException exception) {
        return new RadioSourceException(exception.code(), exception.recoverable(),
                exception.getMessage(), exception);
    }
}
