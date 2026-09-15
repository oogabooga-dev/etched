package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.radio.RadioUrlValidator;

import java.util.Optional;

/** Testable state for the legacy-packet radio editor. */
final class RadioEditState {

    private boolean loaded;
    private boolean configured;
    private PlaybackControlState playbackState = PlaybackControlState.UNKNOWN;
    private String value = "";
    private RadioUrlValidator.Result validation = RadioUrlValidator.validate("");

    boolean receiveInitialUrl(String value) {
        if (this.loaded) {
            return false;
        }
        this.loaded = true;
        this.update(value);
        this.configured = value != null && !value.isBlank();
        return true;
    }

    void update(String value) {
        this.value = value == null ? "" : value;
        this.validation = RadioUrlValidator.validate(this.value);
    }

    Optional<String> play() {
        if (!this.canPlay()) {
            return Optional.empty();
        }
        this.configured = true;
        this.playbackState = PlaybackControlState.STARTING;
        return Optional.of(this.validation.normalized());
    }

    boolean stop() {
        if (!this.canStop()) {
            return false;
        }
        this.playbackState = PlaybackControlState.STOPPING;
        return true;
    }

    void receivePlaybackState(boolean started) {
        boolean stale = started
                ? this.playbackState == PlaybackControlState.STOPPING
                : this.playbackState == PlaybackControlState.STARTING;
        if (!stale) {
            this.playbackState = started
                    ? PlaybackControlState.STARTED
                    : PlaybackControlState.STOPPED;
        }
    }

    boolean loaded() {
        return this.loaded;
    }

    boolean valid() {
        return this.validation.valid();
    }

    boolean canPlay() {
        return this.loaded && this.playbackState == PlaybackControlState.STOPPED
                && this.validation.valid() && !this.validation.normalized().isEmpty();
    }

    boolean canStop() {
        return this.loaded && this.configured;
    }

    String value() {
        return this.value;
    }

    private enum PlaybackControlState {
        UNKNOWN,
        STOPPED,
        STARTING,
        STARTED,
        STOPPING
    }
}
