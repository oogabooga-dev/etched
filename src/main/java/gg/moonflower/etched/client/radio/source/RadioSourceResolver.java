package gg.moonflower.etched.client.radio.source;

import java.net.URI;

public interface RadioSourceResolver {

    boolean supports(URI input);

    RadioResolvedSource resolve(URI input, RadioResolveContext context) throws RadioSourceException;
}
