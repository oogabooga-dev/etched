package gg.moonflower.etched.client.radio.source;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public final class DirectRadioProgramResolver implements RadioSourceProgramResolver {

    private final DirectRadioSourceResolver direct;

    public DirectRadioProgramResolver() {
        this(new DirectRadioSourceResolver());
    }

    DirectRadioProgramResolver(DirectRadioSourceResolver direct) {
        this.direct = Objects.requireNonNull(direct, "direct");
    }

    @Override
    public boolean supports(URI input) {
        return this.direct.supports(input);
    }

    @Override
    public RadioSourceProgram resolveProgram(URI input, RadioResolveContext context)
            throws RadioSourceException {
        Objects.requireNonNull(context, "context");
        if (!this.supports(input)) {
            throw new RadioSourceException(gg.moonflower.etched.client.radio.RadioFailure.Code.INVALID_URL,
                    false, "Direct radio sources must use an absolute HTTP(S) URL", null);
        }
        return new RadioSourceProgram(RadioSourceProgram.Kind.STATION, input, List.of(
                new RadioSourceProgram.Track(input, null, next -> this.direct.resolve(input, next))));
    }
}
