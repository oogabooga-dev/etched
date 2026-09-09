package gg.moonflower.etched.client.radio;

import gg.moonflower.etched.common.radio.RadioConfiguration;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioPlaybackManagerTest {

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static final ResourceKey<Level> FIRST_DIMENSION = dimension("first");
    private static final ResourceKey<Level> SECOND_DIMENSION = dimension("second");
    private static final RadioConfiguration ENABLED =
            new RadioConfiguration("https://radio.example/live", false);

    @Test
    void deduplicatesConfigurationAndAppliesMeaningfulChanges() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey key = new RadioKey(FIRST_DIMENSION, new BlockPos(1, 2, 3));

        assertTrue(manager.update(key, ENABLED));
        assertFalse(manager.update(key, ENABLED));
        assertTrue(manager.update(key, new RadioConfiguration(ENABLED.url(), true)));

        assertEquals(2, driver.applied.size());
        assertEquals(new RadioConfiguration(ENABLED.url(), true), manager.getConfiguration(key).orElseThrow());
    }

    @Test
    void removesRadiosIdempotently() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        manager.update(key, ENABLED);

        assertTrue(manager.remove(key));
        assertFalse(manager.remove(key));

        assertEquals(List.of(key), driver.stopped);
        assertTrue(manager.getConfiguration(key).isEmpty());
    }

    @Test
    void keepsEqualPositionsInDifferentDimensionsIndependent() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey first = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        RadioKey second = new RadioKey(SECOND_DIMENSION, BlockPos.ZERO);

        manager.update(first, ENABLED);
        manager.update(second, ENABLED);
        manager.remove(first);

        assertTrue(manager.getConfiguration(first).isEmpty());
        assertEquals(ENABLED, manager.getConfiguration(second).orElseThrow());
    }

    @Test
    void tickSelfHealsMissedUpdatesWithoutRestartingKnownRadio() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        manager.tick(key, ENABLED);
        manager.tick(key, ENABLED);

        assertEquals(1, driver.applied.size());
        assertEquals(List.of(key, key), driver.ticked);
    }

    @Test
    void reportsActualDriverStateOnlyForTrackedRadios() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey tracked = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        RadioKey missing = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO.above());
        manager.update(tracked, ENABLED);

        assertTrue(manager.isPlaying(tracked));
        driver.playing.add(missing);
        assertFalse(manager.isPlaying(missing));
    }

    @Test
    void clearStopsEveryRadioAndIsIdempotent() {
        RecordingDriver driver = new RecordingDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(driver);
        RadioKey first = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        RadioKey second = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO.above());
        manager.update(first, ENABLED);
        manager.update(second, ENABLED);

        manager.clearAll();
        manager.clearAll();

        assertEquals(Set.of(first, second), new HashSet<>(driver.stopped));
        assertEquals(2, driver.stopped.size());
        assertTrue(manager.getConfiguration(first).isEmpty());
        assertTrue(manager.getConfiguration(second).isEmpty());
    }

    @Test
    void keyCopiesMutablePosition() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(1, 2, 3);
        RadioKey key = new RadioKey(FIRST_DIMENSION, mutable);

        mutable.set(9, 8, 7);

        assertEquals(new BlockPos(1, 2, 3), key.pos());
    }

    private static ResourceKey<Level> dimension(String path) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation("etched_test", path));
    }

    private static final class RecordingDriver implements RadioPlaybackManager.PlaybackDriver {

        private final List<AppliedConfiguration> applied = new ArrayList<>();
        private final List<RadioKey> stopped = new ArrayList<>();
        private final List<RadioKey> ticked = new ArrayList<>();
        private final Set<RadioKey> playing = new HashSet<>();

        @Override
        public void apply(RadioKey key, RadioConfiguration configuration) {
            this.applied.add(new AppliedConfiguration(key, configuration));
            if (configuration.isEnabled()) {
                this.playing.add(key);
            } else {
                this.playing.remove(key);
            }
        }

        @Override
        public void stop(RadioKey key) {
            this.stopped.add(key);
            this.playing.remove(key);
        }

        @Override
        public void tick(RadioKey key, RadioConfiguration configuration) {
            this.ticked.add(key);
        }

        @Override
        public boolean isPlaying(RadioKey key) {
            return this.playing.contains(key);
        }
    }

    private record AppliedConfiguration(RadioKey key, RadioConfiguration configuration) {
    }
}
