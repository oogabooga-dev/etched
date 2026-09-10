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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(RadioPlaybackState.STOPPED,
                manager.getSessionSnapshot(key).orElseThrow().state());
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

    @Test
    void replacementAndRedstoneCancelThePreviousGeneration() {
        RecordingDriver playback = new RecordingDriver();
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(playback, sessions);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        manager.update(key, ENABLED);
        StartedSession first = sessions.started.get(0);
        assertFalse(manager.update(key, ENABLED));
        assertFalse(first.attempt().cancellation().isCancelled());
        manager.update(key, new RadioConfiguration("https://radio.example/new", false));
        StartedSession second = sessions.started.get(1);

        assertTrue(first.attempt().cancellation().isCancelled());
        assertFalse(first.session().advance(first.attempt().generation(), RadioPlaybackState.CONNECTING));
        assertFalse(second.attempt().cancellation().isCancelled());

        manager.update(key, new RadioConfiguration(second.configuration().url(), true));

        assertTrue(second.attempt().cancellation().isCancelled());
        assertEquals(2, sessions.started.size());
        assertEquals(RadioPlaybackState.STOPPED,
                manager.getSessionSnapshot(key).orElseThrow().state());
    }

    @Test
    void normalizesSurroundingUrlWhitespaceBeforeStartingASession() {
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(new RecordingDriver(), sessions);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        manager.update(key, new RadioConfiguration("   ", false));
        manager.update(key, new RadioConfiguration("  https://radio.example/live  ", false));

        assertEquals(1, sessions.started.size());
        assertEquals("https://radio.example/live", sessions.started.get(0).attempt().source());
    }

    @Test
    void removeAndClearCancelEveryOwnedSession() {
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RadioPlaybackManager manager = new RadioPlaybackManager(new RecordingDriver(), sessions);
        RadioKey firstKey = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        RadioKey secondKey = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO.above());
        manager.update(firstKey, ENABLED);
        manager.update(secondKey, ENABLED);
        StartedSession first = sessions.started.get(0);
        StartedSession second = sessions.started.get(1);

        manager.remove(firstKey);
        manager.clearAll();

        assertTrue(first.attempt().cancellation().isCancelled());
        assertTrue(second.attempt().cancellation().isCancelled());
        assertEquals(Set.of(firstKey, secondKey), new HashSet<>(sessions.stopped));
    }

    @Test
    void sessionBackendIsExclusiveAndReportsOnlyPlayingState() {
        RecordingDriver playback = new RecordingDriver();
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RecordingEffects effects = new RecordingEffects();
        RadioPlaybackManager manager = new RadioPlaybackManager(playback, sessions, effects);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        manager.update(key, ENABLED);
        StartedSession started = sessions.started.get(0);

        assertTrue(playback.applied.isEmpty());
        assertFalse(manager.isPlaying(key));
        assertTrue(started.session().advance(started.attempt().generation(), RadioPlaybackState.CONNECTING));
        assertFalse(manager.isPlaying(key));
        assertTrue(started.session().advance(started.attempt().generation(), RadioPlaybackState.BUFFERING));
        assertFalse(manager.isPlaying(key));
        assertTrue(started.session().advance(started.attempt().generation(), RadioPlaybackState.PLAYING));
        assertTrue(manager.isPlaying(key));

        manager.tick(key, ENABLED);
        assertEquals(RadioPlaybackState.PLAYING, effects.updated.get(effects.updated.size() - 1).snapshot().state());
        manager.remove(key);

        assertTrue(playback.stopped.isEmpty());
        assertEquals(List.of(key), effects.stopped);
        assertFalse(manager.isPlaying(key));
    }

    @Test
    void drainsMetadataToEffectsAndRejectsDetachedSessionUpdates() {
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RecordingEffects effects = new RecordingEffects();
        RadioPlaybackManager manager = new RadioPlaybackManager(new RecordingDriver(), sessions, effects);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        manager.update(key, ENABLED);
        StartedSession first = sessions.started.get(0);
        assertTrue(first.session().advance(first.attempt().generation(), RadioPlaybackState.CONNECTING));
        assertTrue(first.session().advance(first.attempt().generation(), RadioPlaybackState.BUFFERING));
        assertTrue(first.session().offerStreamTitle(first.attempt(), "Current title"));

        manager.tick(key, ENABLED);
        EffectUpdate buffered = effects.updated.get(effects.updated.size() - 1);
        assertEquals(RadioPlaybackState.BUFFERING, buffered.snapshot().state());
        assertEquals("Current title", buffered.snapshot().streamTitle());

        assertTrue(first.session().advance(first.attempt().generation(), RadioPlaybackState.PLAYING));
        manager.tick(key, ENABLED);
        EffectUpdate playing = effects.updated.get(effects.updated.size() - 1);
        assertEquals(RadioPlaybackState.PLAYING, playing.snapshot().state());
        assertEquals("Current title", playing.snapshot().streamTitle());

        manager.update(key, new RadioConfiguration("https://radio.example/replacement", false));
        StartedSession second = sessions.started.get(1);
        assertEquals(first.attempt().generation(), second.attempt().generation());
        assertFalse(first.session().offerStreamTitle(first.attempt(), "Stale title"));
        assertFalse(second.session().offerStreamTitle(first.attempt(), "Stale title"));
        manager.tick(key, second.configuration());
        assertNull(manager.getSessionSnapshot(key).orElseThrow().streamTitle());
    }

    @Test
    void startFailureLeavesTheRadioRetryableAndClearsEffects() {
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        sessions.throwOnStart = true;
        RecordingEffects effects = new RecordingEffects();
        RadioPlaybackManager manager = new RadioPlaybackManager(new RecordingDriver(), sessions, effects);
        RadioKey key = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);

        assertThrows(IllegalStateException.class, () -> manager.update(key, ENABLED));

        assertTrue(manager.getConfiguration(key).isEmpty());
        assertEquals(List.of(key), effects.stopped);
        sessions.throwOnStart = false;
        assertTrue(manager.update(key, ENABLED));
        assertEquals(1, sessions.started.size());
    }

    @Test
    void stopFailureCannotOrphanEffectsOrOtherRadiosDuringClear() {
        RecordingSessionDriver sessions = new RecordingSessionDriver();
        RecordingEffects effects = new RecordingEffects();
        RadioPlaybackManager manager = new RadioPlaybackManager(new RecordingDriver(), sessions, effects);
        RadioKey first = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO);
        RadioKey second = new RadioKey(FIRST_DIMENSION, BlockPos.ZERO.above());
        manager.update(first, ENABLED);
        manager.update(second, ENABLED);
        sessions.throwOnStop = true;

        assertThrows(IllegalStateException.class, manager::clearAll);

        assertEquals(Set.of(first, second), new HashSet<>(effects.stopped));
        assertTrue(manager.getConfiguration(first).isEmpty());
        assertTrue(manager.getConfiguration(second).isEmpty());
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

    private static final class RecordingSessionDriver implements RadioPlaybackManager.SessionDriver {

        private final List<StartedSession> started = new ArrayList<>();
        private final List<RadioKey> stopped = new ArrayList<>();
        private boolean throwOnStart;
        private boolean throwOnStop;

        @Override
        public void start(RadioKey key, RadioConfiguration configuration, RadioSession session,
                          RadioSession.Attempt attempt) {
            if (this.throwOnStart) {
                throw new IllegalStateException("start failed");
            }
            this.started.add(new StartedSession(key, configuration, session, attempt));
        }

        @Override
        public void stop(RadioKey key, RadioSession session) {
            StartedSession startedSession = this.started.stream()
                    .filter(started -> started.session() == session)
                    .findFirst()
                    .orElse(null);
            if (startedSession != null) {
                assertTrue(startedSession.attempt().cancellation().isCancelled());
            }
            this.stopped.add(key);
            if (this.throwOnStop) {
                throw new IllegalStateException("stop failed");
            }
        }
    }

    private static final class RecordingEffects implements RadioPlaybackEffects {

        private final List<EffectUpdate> updated = new ArrayList<>();
        private final List<RadioKey> stopped = new ArrayList<>();

        @Override
        public void update(RadioKey key, RadioSession.Snapshot snapshot) {
            this.updated.add(new EffectUpdate(key, snapshot));
        }

        @Override
        public void stop(RadioKey key) {
            this.stopped.add(key);
        }
    }

    private record AppliedConfiguration(RadioKey key, RadioConfiguration configuration) {
    }

    private record StartedSession(RadioKey key, RadioConfiguration configuration, RadioSession session,
                                  RadioSession.Attempt attempt) {
    }

    private record EffectUpdate(RadioKey key, RadioSession.Snapshot snapshot) {
    }
}
