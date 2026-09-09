package gg.moonflower.etched.client.radio.source;

public record RadioResolveLimits(int sniffBytes, int maxPlaylistBytes, int maxPlaylistEntries,
                                 int maxLineLength, int maxPlaylistDepth, int maxResolutionSteps) {

    public static final RadioResolveLimits DEFAULT = new RadioResolveLimits(
            8192, 256 * 1024, 100, 8192, 3, 128);

    public RadioResolveLimits {
        if (sniffBytes < 4 || maxPlaylistBytes < 1 || maxPlaylistEntries < 1
                || maxLineLength < 1 || maxPlaylistDepth < 0 || maxResolutionSteps < 1) {
            throw new IllegalArgumentException("Radio source limits must be positive");
        }
        if (sniffBytes > maxPlaylistBytes) {
            throw new IllegalArgumentException("The sniff limit cannot exceed the playlist body limit");
        }
    }
}
