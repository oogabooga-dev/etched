package gg.moonflower.etched.client.radio;

import gg.moonflower.etched.common.radio.RadioClientBridge;
import gg.moonflower.etched.common.radio.RadioConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns the client-side set of loaded radios and deduplicates configuration changes.
 */
public final class RadioPlaybackManager implements RadioClientBridge.Listener {

    private static final RadioPlaybackManager INSTANCE = new RadioPlaybackManager(new LegacyRadioPlaybackDriver());

    private final Map<RadioKey, ManagedRadio> radios;
    private final PlaybackDriver playback;
    private final SessionDriver sessions;

    RadioPlaybackManager(PlaybackDriver playback) {
        this(playback, SessionDriver.NOOP);
    }

    RadioPlaybackManager(PlaybackDriver playback, SessionDriver sessions) {
        this.radios = new HashMap<>();
        this.playback = Objects.requireNonNull(playback, "playback");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }

    public static RadioPlaybackManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void update(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration) {
        this.update(new RadioKey(dimension, pos), configuration);
    }

    public boolean update(RadioKey key, RadioConfiguration configuration) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(configuration, "configuration");
        ManagedRadio previous = this.radios.get(key);
        if (previous != null && configuration.equals(previous.configuration())) {
            return false;
        }

        if (previous != null) {
            this.stopSession(key, previous.session());
        }
        RadioSession session = new RadioSession();
        this.radios.put(key, new ManagedRadio(configuration, session));
        String source = configuration.url().trim();
        if (!source.isEmpty() && !configuration.powered() && this.sessions.enabled()) {
            this.sessions.start(key, configuration, session, session.start(source));
        }
        this.playback.apply(key, configuration);
        return true;
    }

    @Override
    public void remove(ResourceKey<Level> dimension, BlockPos pos) {
        this.remove(new RadioKey(dimension, pos));
    }

    public boolean remove(RadioKey key) {
        Objects.requireNonNull(key, "key");
        ManagedRadio removed = this.radios.remove(key);
        if (removed == null) {
            return false;
        }

        this.stopSession(key, removed.session());
        this.playback.stop(key);
        return true;
    }

    @Override
    public void tick(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration) {
        this.tick(new RadioKey(dimension, pos), configuration);
    }

    public void tick(RadioKey key, RadioConfiguration configuration) {
        this.update(key, configuration);
        this.playback.tick(key, configuration);
    }

    @Override
    public boolean isPlaying(ResourceKey<Level> dimension, BlockPos pos) {
        return this.isPlaying(new RadioKey(dimension, pos));
    }

    public boolean isPlaying(RadioKey key) {
        return this.radios.containsKey(key) && this.playback.isPlaying(key);
    }

    public Optional<RadioConfiguration> getConfiguration(RadioKey key) {
        ManagedRadio radio = this.radios.get(key);
        return radio == null ? Optional.empty() : Optional.of(radio.configuration());
    }

    public Optional<RadioSession.Snapshot> getSessionSnapshot(RadioKey key) {
        ManagedRadio radio = this.radios.get(Objects.requireNonNull(key, "key"));
        return radio == null ? Optional.empty() : Optional.of(radio.session().snapshot());
    }

    public void clearAll() {
        ArrayList<Map.Entry<RadioKey, ManagedRadio>> entries = new ArrayList<>(this.radios.entrySet());
        this.radios.clear();
        for (Map.Entry<RadioKey, ManagedRadio> entry : entries) {
            this.stopSession(entry.getKey(), entry.getValue().session());
            this.playback.stop(entry.getKey());
        }
    }

    private void stopSession(RadioKey key, RadioSession session) {
        session.stop();
        this.sessions.stop(key, session);
    }

    interface PlaybackDriver {

        void apply(RadioKey key, RadioConfiguration configuration);

        void stop(RadioKey key);

        void tick(RadioKey key, RadioConfiguration configuration);

        boolean isPlaying(RadioKey key);
    }

    interface SessionDriver {

        SessionDriver NOOP = new SessionDriver() {
            @Override
            public boolean enabled() {
                return false;
            }

            @Override
            public void start(RadioKey key, RadioConfiguration configuration, RadioSession session,
                              RadioSession.Attempt attempt) {
            }

            @Override
            public void stop(RadioKey key, RadioSession session) {
            }
        };

        default boolean enabled() {
            return true;
        }

        void start(RadioKey key, RadioConfiguration configuration, RadioSession session,
                   RadioSession.Attempt attempt);

        void stop(RadioKey key, RadioSession session);
    }

    private record ManagedRadio(RadioConfiguration configuration, RadioSession session) {
    }
}
