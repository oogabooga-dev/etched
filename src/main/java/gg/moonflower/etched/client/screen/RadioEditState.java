package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.radio.RadioUrlValidator;

import java.util.Optional;

/** Testable state for the legacy-packet radio editor. */
final class RadioEditState {

    private boolean loaded;
    private boolean configured;
    private boolean playbackStateKnown;
    private boolean playbackStarted;
    private boolean startPending;
    private boolean stopPending;
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
        this.startPending = true;
        this.stopPending = false;
        return Optional.of(this.validation.normalized());
    }

    boolean stop() {
        if (!this.canStop()) {
            return false;
        }
        this.startPending = false;
        this.stopPending = true;
        return true;
    }

    void receivePlaybackState(boolean started) {
        this.playbackStateKnown = true;
        this.playbackStarted = started;
        if (started) {
            this.startPending = false;
        } else {
            this.stopPending = false;
        }
    }

    boolean loaded() {
        return this.loaded;
    }

    boolean valid() {
        return this.validation.valid();
    }

    boolean canPlay() {
        return this.loaded && this.playbackStateKnown && !this.playbackStarted
                && !this.startPending && !this.stopPending
                && this.validation.valid() && !this.validation.normalized().isEmpty();
    }

    boolean canStop() {
        return this.loaded && this.configured;
    }

    String value() {
        return this.value;
    }
}
