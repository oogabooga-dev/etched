package gg.moonflower.etched.client.radio;

/** Applies client-only presentation state for session-backed radio playback. */
interface RadioPlaybackEffects {

    RadioPlaybackEffects NOOP = new RadioPlaybackEffects() {
        @Override
        public void update(RadioKey key, RadioSession.Snapshot snapshot) {
        }

        @Override
        public void stop(RadioKey key) {
        }
    };

    void update(RadioKey key, RadioSession.Snapshot snapshot);

    void stop(RadioKey key);
}
