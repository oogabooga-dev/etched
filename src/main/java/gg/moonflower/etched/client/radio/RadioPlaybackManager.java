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

    private static final RadioPlaybackManager INSTANCE = new RadioPlaybackManager(
            new LegacyRadioPlaybackDriver(), SessionDriver.NOOP, new MinecraftRadioPlaybackEffects());

    private final Map<RadioKey, ManagedRadio> radios;
    private final PlaybackDriver playback;
    private final SessionDriver sessions;
    private final RadioPlaybackEffects effects;

    RadioPlaybackManager(PlaybackDriver playback) {
        this(playback, SessionDriver.NOOP, RadioPlaybackEffects.NOOP);
    }

    RadioPlaybackManager(PlaybackDriver playback, SessionDriver sessions) {
        this(playback, sessions, RadioPlaybackEffects.NOOP);
    }

    RadioPlaybackManager(PlaybackDriver playback, SessionDriver sessions, RadioPlaybackEffects effects) {
        this.radios = new HashMap<>();
        this.playback = Objects.requireNonNull(playback, "playback");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.effects = Objects.requireNonNull(effects, "effects");
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
            this.radios.remove(key);
            this.stopSession(key, previous.session());
        }
        RadioSession session = new RadioSession();
        ManagedRadio radio = new ManagedRadio(configuration, session);
        this.radios.put(key, radio);
        String source = configuration.url().trim();
        if (this.sessions.enabled()) {
            if (!source.isEmpty() && !configuration.powered()) {
                try {
                    this.sessions.start(key, configuration, session, session.start(source));
                } catch (RuntimeException exception) {
                    this.radios.remove(key, radio);
                    try {
                        this.stopSession(key, session);
                    } catch (RuntimeException stopException) {
                        exception.addSuppressed(stopException);
                    }
                    throw exception;
                }
            }
            this.effects.update(key, session.snapshot());
        } else {
            this.playback.apply(key, configuration);
        }
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

        if (this.sessions.enabled()) {
            this.stopSession(key, removed.session());
        } else {
            removed.session().stop();
            this.playback.stop(key);
        }
        return true;
    }

    @Override
    public void tick(ResourceKey<Level> dimension, BlockPos pos, RadioConfiguration configuration) {
        this.tick(new RadioKey(dimension, pos), configuration);
    }

    public void tick(RadioKey key, RadioConfiguration configuration) {
        this.update(key, configuration);
        if (this.sessions.enabled()) {
            ManagedRadio radio = this.radios.get(key);
            if (radio != null) {
                radio.session().applyPendingStreamTitle();
                this.effects.update(key, radio.session().snapshot());
            }
        } else {
            this.playback.tick(key, configuration);
        }
    }

    @Override
    public boolean isPlaying(ResourceKey<Level> dimension, BlockPos pos) {
        return this.isPlaying(new RadioKey(dimension, pos));
    }

    public boolean isPlaying(RadioKey key) {
        ManagedRadio radio = this.radios.get(key);
        if (radio == null) {
            return false;
        }
        return this.sessions.enabled()
                ? radio.session().snapshot().state() == RadioPlaybackState.PLAYING
                : this.playback.isPlaying(key);
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
        RuntimeException failure = null;
        for (Map.Entry<RadioKey, ManagedRadio> entry : entries) {
            try {
                if (this.sessions.enabled()) {
                    this.stopSession(entry.getKey(), entry.getValue().session());
                } else {
                    entry.getValue().session().stop();
                    this.playback.stop(entry.getKey());
                }
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void stopSession(RadioKey key, RadioSession session) {
        session.stop();
        if (this.sessions.enabled()) {
            try {
                this.sessions.stop(key, session);
            } finally {
                this.effects.stop(key);
            }
        }
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
