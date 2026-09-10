package gg.moonflower.etched.client.radio;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Thread-safe lifecycle state for one client-side radio.
 *
 * <p>Every start or retry creates a new generation. Asynchronous work must
 * include that generation when reporting progress so stale work is ignored.</p>
 */
public final class RadioSession {

    private long generation;
    private String source = "";
    private RadioPlaybackState state = RadioPlaybackState.STOPPED;
    private RadioFailure failure;
    private RadioCancellation cancellation;
    private String streamTitle;
    private PendingStreamTitle pendingStreamTitle;

    public Attempt start(String source) {
        Objects.requireNonNull(source, "source");
        if (source.isBlank()) {
            throw new IllegalArgumentException("Radio source must not be blank");
        }

        RadioCancellation previous;
        Attempt attempt;
        synchronized (this) {
            previous = this.cancellation;
            attempt = this.beginAttempt(source);
        }
        cancel(previous);
        return attempt;
    }

    public synchronized boolean advance(long generation, RadioPlaybackState nextState) {
        Objects.requireNonNull(nextState, "nextState");
        if (nextState != RadioPlaybackState.CONNECTING
                && nextState != RadioPlaybackState.BUFFERING
                && nextState != RadioPlaybackState.PLAYING) {
            throw new IllegalArgumentException("Not a progress state: " + nextState);
        }
        if (!this.isCurrentAttempt(generation)) {
            return false;
        }
        if (!isAllowedProgression(this.state, nextState)) {
            throw new IllegalStateException("Cannot transition radio from " + this.state + " to " + nextState);
        }

        this.state = nextState;
        return true;
    }

    public Optional<ReconnectWait> scheduleReconnect(long generation, RadioFailure failure) {
        Objects.requireNonNull(failure, "failure");
        if (!failure.recoverable()) {
            throw new IllegalArgumentException("Reconnect requires a recoverable failure");
        }

        RadioCancellation previous;
        ReconnectWait wait;
        synchronized (this) {
            if (!this.isCurrentAttempt(generation)) {
                return Optional.empty();
            }

            this.state = RadioPlaybackState.RECONNECT_WAIT;
            this.failure = failure;
            this.pendingStreamTitle = null;
            previous = this.cancellation;
            this.cancellation = new RadioCancellation();
            wait = new ReconnectWait(this.generation, this.cancellation);
        }
        cancel(previous);
        return Optional.of(wait);
    }

    public boolean fail(long generation, RadioFailure failure) {
        Objects.requireNonNull(failure, "failure");
        if (failure.recoverable()) {
            throw new IllegalArgumentException("Failed state requires a fatal failure");
        }
        return this.finishAttempt(generation, RadioPlaybackState.FAILED, failure);
    }

    public Optional<Attempt> retry(long expectedGeneration) {
        RadioCancellation previous;
        Attempt attempt;
        synchronized (this) {
            if (this.generation != expectedGeneration
                    || this.state != RadioPlaybackState.RECONNECT_WAIT && this.state != RadioPlaybackState.FAILED) {
                return Optional.empty();
            }
            previous = this.cancellation;
            attempt = this.beginAttempt(this.source);
        }
        cancel(previous);
        return Optional.of(attempt);
    }

    public boolean stop() {
        RadioCancellation previous;
        synchronized (this) {
            if (this.state == RadioPlaybackState.STOPPED) {
                return false;
            }

            this.generation++;
            this.state = RadioPlaybackState.STOPPED;
            this.failure = null;
            this.streamTitle = null;
            this.pendingStreamTitle = null;
            previous = this.cancellation;
            this.cancellation = null;
        }
        cancel(previous);
        return true;
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(this.generation, this.source, this.state, this.failure, this.streamTitle);
    }

    /** Coalesces metadata produced by the exact currently active stream attempt. */
    public synchronized boolean offerStreamTitle(Attempt attempt, String streamTitle) {
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(streamTitle, "streamTitle");
        if (!this.isCurrentAttempt(attempt)) {
            return false;
        }
        this.pendingStreamTitle = new PendingStreamTitle(
                attempt.generation(), attempt.cancellation(), streamTitle);
        return true;
    }

    /** Applies at most one latest metadata update from the client tick. */
    public synchronized boolean applyPendingStreamTitle() {
        PendingStreamTitle pending = this.pendingStreamTitle;
        this.pendingStreamTitle = null;
        if (pending == null || !this.isCurrentAttempt(pending.generation(), pending.cancellation())) {
            return false;
        }
        String nextTitle = pending.streamTitle().isEmpty() ? null : pending.streamTitle();
        if (Objects.equals(this.streamTitle, nextTitle)) {
            return false;
        }
        this.streamTitle = nextTitle;
        return true;
    }

    private boolean finishAttempt(long generation, RadioPlaybackState nextState, RadioFailure failure) {
        RadioCancellation previous;
        synchronized (this) {
            if (!this.isCurrentAttempt(generation)) {
                return false;
            }

            this.state = nextState;
            this.failure = failure;
            this.pendingStreamTitle = null;
            previous = this.cancellation;
            this.cancellation = null;
        }
        cancel(previous);
        return true;
    }

    private boolean isCurrent(long generation) {
        return this.generation == generation
                && this.cancellation != null
                && !this.cancellation.isCancelled();
    }

    private boolean isCurrentAttempt(long generation) {
        return this.isCurrent(generation) && switch (this.state) {
            case RESOLVING, CONNECTING, BUFFERING, PLAYING -> true;
            default -> false;
        };
    }

    private boolean isCurrentAttempt(Attempt attempt) {
        return this.isCurrentAttempt(attempt.generation(), attempt.cancellation());
    }

    private boolean isCurrentAttempt(long generation, RadioCancellation cancellation) {
        return this.generation == generation
                && this.cancellation == cancellation
                && !cancellation.isCancelled()
                && switch (this.state) {
            case RESOLVING, CONNECTING, BUFFERING, PLAYING -> true;
            default -> false;
        };
    }

    private Attempt beginAttempt(String source) {
        this.generation++;
        this.source = source;
        this.state = RadioPlaybackState.RESOLVING;
        this.failure = null;
        this.streamTitle = null;
        this.pendingStreamTitle = null;
        this.cancellation = new RadioCancellation();
        return new Attempt(this.generation, this.source, this.cancellation);
    }

    private static boolean isAllowedProgression(RadioPlaybackState current, RadioPlaybackState next) {
        return switch (current) {
            case RESOLVING -> next == RadioPlaybackState.CONNECTING;
            case CONNECTING -> next == RadioPlaybackState.BUFFERING;
            case BUFFERING -> next == RadioPlaybackState.PLAYING;
            default -> false;
        };
    }

    private static void cancel(@Nullable RadioCancellation cancellation) {
        if (cancellation != null) {
            cancellation.cancel();
        }
    }

    public record Attempt(long generation, String source, RadioCancellation cancellation) {

        public Attempt {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(cancellation, "cancellation");
        }
    }

    public record ReconnectWait(long generation, RadioCancellation cancellation) {

        public ReconnectWait {
            Objects.requireNonNull(cancellation, "cancellation");
        }
    }

    public record Snapshot(long generation, String source, RadioPlaybackState state,
                           @Nullable RadioFailure failure, @Nullable String streamTitle) {

        public Snapshot {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(state, "state");
        }
    }

    private record PendingStreamTitle(long generation, RadioCancellation cancellation, String streamTitle) {

        private PendingStreamTitle {
            Objects.requireNonNull(cancellation, "cancellation");
            Objects.requireNonNull(streamTitle, "streamTitle");
        }
    }
}
