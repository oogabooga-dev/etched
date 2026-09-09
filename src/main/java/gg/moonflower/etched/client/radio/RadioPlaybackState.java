package gg.moonflower.etched.client.radio;

/**
 * The client-side lifecycle of one radio playback session.
 */
public enum RadioPlaybackState {
    STOPPED,
    RESOLVING,
    CONNECTING,
    BUFFERING,
    PLAYING,
    RECONNECT_WAIT,
    FAILED
}
