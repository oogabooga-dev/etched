package gg.moonflower.etched.client.radio.source;

import java.net.URI;

/** Resolves a station or an ordered service album without opening shared audio streams. */
public interface RadioSourceProgramResolver {

    boolean supports(URI input);

    RadioSourceProgram resolveProgram(URI input, RadioResolveContext context) throws RadioSourceException;
}
