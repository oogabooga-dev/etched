package gg.moonflower.etched.client.radio;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioSessionTest {

    @Test
    void advancesThroughThePlaybackLifecycle() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");

        assertEquals(RadioPlaybackState.RESOLVING, session.snapshot().state());
        assertTrue(session.advance(attempt.generation(), RadioPlaybackState.CONNECTING));
        assertTrue(session.advance(attempt.generation(), RadioPlaybackState.BUFFERING));
        assertTrue(session.advance(attempt.generation(), RadioPlaybackState.PLAYING));

        RadioSession.Snapshot snapshot = session.snapshot();
        assertEquals(attempt.generation(), snapshot.generation());
        assertEquals("https://radio.example/live", snapshot.source());
        assertEquals(RadioPlaybackState.PLAYING, snapshot.state());
        assertNull(snapshot.failure());
        assertFalse(attempt.cancellation().isCancelled());
    }

    @Test
    void replacementCancelsOldAttemptAndRejectsItsCallbacks() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt oldAttempt = session.start("https://radio.example/old");
        RadioSession.Attempt newAttempt = session.start("https://radio.example/new");

        assertTrue(oldAttempt.cancellation().isCancelled());
        assertFalse(newAttempt.cancellation().isCancelled());
        assertFalse(session.advance(oldAttempt.generation(), RadioPlaybackState.CONNECTING));
        assertTrue(session.advance(newAttempt.generation(), RadioPlaybackState.CONNECTING));
        assertEquals("https://radio.example/new", session.snapshot().source());
    }

    @Test
    void stopIsIdempotentAndInvalidatesCurrentGeneration() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");

        assertTrue(session.stop());
        long stoppedGeneration = session.snapshot().generation();

        assertTrue(attempt.cancellation().isCancelled());
        assertEquals(attempt.generation() + 1, stoppedGeneration);
        assertEquals(RadioPlaybackState.STOPPED, session.snapshot().state());
        assertFalse(session.advance(attempt.generation(), RadioPlaybackState.CONNECTING));
        assertFalse(session.stop());
        assertEquals(stoppedGeneration, session.snapshot().generation());
    }

    @Test
    void recoverableFailureCanStartAComparedRetry() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");
        RadioFailure failure = RadioFailure.recoverable(
                RadioFailure.Code.READ_TIMEOUT, "Station stopped sending data", null);

        RadioSession.ReconnectWait wait = session.scheduleReconnect(attempt.generation(), failure).orElseThrow();
        assertTrue(attempt.cancellation().isCancelled());
        assertFalse(wait.cancellation().isCancelled());
        assertEquals(RadioPlaybackState.RECONNECT_WAIT, session.snapshot().state());
        assertSame(failure, session.snapshot().failure());

        RadioSession.Attempt retry = session.retry(attempt.generation()).orElseThrow();

        assertTrue(wait.cancellation().isCancelled());
        assertEquals(attempt.generation() + 1, retry.generation());
        assertEquals(attempt.source(), retry.source());
        assertEquals(RadioPlaybackState.RESOLVING, session.snapshot().state());
        assertNull(session.snapshot().failure());
        assertTrue(session.retry(attempt.generation()).isEmpty());
    }

    @Test
    void stopCancelsPendingReconnect() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");
        RadioFailure failure = RadioFailure.recoverable(
                RadioFailure.Code.CONNECT_TIMEOUT, "Connection timed out", null);
        RadioSession.ReconnectWait wait = session.scheduleReconnect(attempt.generation(), failure).orElseThrow();

        assertTrue(session.stop());

        assertTrue(wait.cancellation().isCancelled());
        assertEquals(RadioPlaybackState.STOPPED, session.snapshot().state());
        assertTrue(session.retry(attempt.generation()).isEmpty());
        assertFalse(session.advance(attempt.generation(), RadioPlaybackState.CONNECTING));
    }

    @Test
    void fatalFailureWaitsForAnExplicitRetry() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/aac");
        RadioFailure failure = RadioFailure.fatal(
                RadioFailure.Code.UNSUPPORTED_AAC, "AAC is not supported", null);

        assertTrue(session.fail(attempt.generation(), failure));
        assertEquals(RadioPlaybackState.FAILED, session.snapshot().state());
        assertSame(failure, session.snapshot().failure());

        RadioSession.Attempt retry = session.retry(attempt.generation()).orElseThrow();
        assertEquals(attempt.generation() + 1, retry.generation());
        assertEquals(RadioPlaybackState.RESOLVING, session.snapshot().state());
    }

    @Test
    void rejectsSkippedOrControlStateTransitions() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");

        assertThrows(IllegalStateException.class,
                () -> session.advance(attempt.generation(), RadioPlaybackState.PLAYING));
        assertThrows(IllegalArgumentException.class,
                () -> session.advance(attempt.generation(), RadioPlaybackState.FAILED));
    }

    @Test
    void onlyOneCompetingTerminalCallbackWins() throws Exception {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");
        RadioFailure recoverable = RadioFailure.recoverable(
                RadioFailure.Code.READ_TIMEOUT, "Station stopped sending data", null);
        RadioFailure fatal = RadioFailure.fatal(
                RadioFailure.Code.UNSUPPORTED_AUDIO, "Unknown audio format", null);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> reconnect = executor.submit(() -> {
                start.await();
                return session.scheduleReconnect(attempt.generation(), recoverable).isPresent();
            });
            Future<Boolean> fail = executor.submit(() -> {
                start.await();
                return session.fail(attempt.generation(), fatal);
            });

            start.countDown();
            assertEquals(1, List.of(reconnect.get(), fail.get()).stream().filter(Boolean::booleanValue).count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void onlyOneCompetingRetryStartsANewAttempt() throws Exception {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");
        RadioFailure failure = RadioFailure.recoverable(
                RadioFailure.Code.CONNECT_TIMEOUT, "Connection timed out", null);
        session.scheduleReconnect(attempt.generation(), failure).orElseThrow();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Optional<RadioSession.Attempt>>> retries = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            for (int i = 0; i < 4; i++) {
                retries.add(executor.submit(() -> {
                    start.await();
                    return session.retry(attempt.generation());
                }));
            }

            start.countDown();
            int started = 0;
            for (Future<Optional<RadioSession.Attempt>> retry : retries) {
                if (retry.get().isPresent()) {
                    started++;
                }
            }
            assertEquals(1, started);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejectsRecoverableFailureAsTerminal() {
        RadioSession session = new RadioSession();
        RadioSession.Attempt attempt = session.start("https://radio.example/live");
        RadioFailure failure = RadioFailure.recoverable(
                RadioFailure.Code.READ_TIMEOUT, "Station stopped sending data", null);

        assertThrows(IllegalArgumentException.class, () -> session.fail(attempt.generation(), failure));
    }

    @Test
    void rejectsBlankSource() {
        RadioSession session = new RadioSession();

        assertThrows(IllegalArgumentException.class, () -> session.start("  "));
    }
}
