package gg.moonflower.etched.client.screen;

import gg.moonflower.etched.common.radio.RadioUrlValidator;

import java.util.Optional;

/** Testable state for the legacy-packet radio editor. */
final class RadioEditState {

    private boolean loaded;
    private boolean submitted;
    private String value = "";
    private RadioUrlValidator.Result validation = RadioUrlValidator.validate("");

    boolean receiveInitialUrl(String value) {
        if (this.loaded || this.submitted) {
            return false;
        }
        this.loaded = true;
        this.update(value);
        return true;
    }

    void update(String value) {
        if (this.submitted) {
            return;
        }
        this.value = value == null ? "" : value;
        this.validation = RadioUrlValidator.validate(this.value);
    }

    Optional<String> submit() {
        if (!this.canSubmit()) {
            return Optional.empty();
        }
        this.submitted = true;
        return Optional.of(this.validation.normalized());
    }

    boolean loaded() {
        return this.loaded;
    }

    boolean valid() {
        return this.validation.valid();
    }

    boolean canSubmit() {
        return this.loaded && !this.submitted && this.validation.valid();
    }

    String value() {
        return this.value;
    }
}
