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

    private final Map<RadioKey, RadioConfiguration> radios;
    private final PlaybackDriver playback;

    RadioPlaybackManager(PlaybackDriver playback) {
        this.radios = new HashMap<>();
        this.playback = Objects.requireNonNull(playback, "playback");
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
        RadioConfiguration previous = this.radios.put(key, configuration);
        if (configuration.equals(previous)) {
            return false;
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
        if (this.radios.remove(key) == null) {
            return false;
        }

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
        return Optional.ofNullable(this.radios.get(key));
    }

    public void clearAll() {
        ArrayList<RadioKey> keys = new ArrayList<>(this.radios.keySet());
        this.radios.clear();
        keys.forEach(this.playback::stop);
    }

    interface PlaybackDriver {

        void apply(RadioKey key, RadioConfiguration configuration);

        void stop(RadioKey key);

        void tick(RadioKey key, RadioConfiguration configuration);

        boolean isPlaying(RadioKey key);
    }
}
