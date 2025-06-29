package io.github.unjoinable.veltrix.fsm;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class representing a state in a finite state machine.
 *
 * <p>This class provides lifecycle management for states with the following phases:
 * <ul>
 *   <li><strong>Start:</strong> Initialize the state and begin execution</li>
 *   <li><strong>Update:</strong> Continuously process state logic until ready to end</li>
 *   <li><strong>End:</strong> Cleanup and finalize the state</li>
 * </ul>
 *
 * <p>States have a configurable duration and can be frozen to prevent automatic ending.
 * All state transitions are thread-safe and exception-safe.
 *
 * @author Minikloon (code transalated from Kotlin)
 */
public abstract class State {

    private static final Logger LOGGER = Logger.getLogger(State.class.getName());

    private volatile boolean started = false;
    private volatile boolean ended = false;

    /**
     * Prevents the state from ending automatically when duration expires.
     * Must be manually set to false to allow the state to end.
     */
    protected volatile boolean frozen = false;

    private @Nullable Instant startInstant;
    private final ReentrantLock stateLock = new ReentrantLock();
    private volatile boolean updating = false;

    /**
     * Starts this state if it hasn't been started or ended already.
     * This method is thread-safe and idempotent.
     *
     * <p>The start process:
     * <ol>
     *   <li>Checks if state can be started (not already started or ended)</li>
     *   <li>Records the start time</li>
     *   <li>Calls the {@link #onStart()} hook</li>
     * </ol>
     *
     * <p>Any exceptions thrown during {@link #onStart()} are caught and logged.
     */
    public final void start() {
        stateLock.lock();
        try {
            if (started || ended) {
                return;
            }
            started = true;
            startInstant = Instant.now();
        } finally {
            stateLock.unlock();
        }

        try {
            onStart();
            LOGGER.info(() -> "State " + getClass().getSimpleName() + " started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Exception during " + getClass().getSimpleName() + " start");
        }
    }

    /**
     * Hook method called when the state starts.
     * Subclasses should override this to implement state-specific initialization logic.
     *
     * @throws Exception if an error occurs during state initialization
     */
    protected abstract void onStart() throws Exception;

    /**
     * Updates this state's logic and checks if it should end.
     * This method is thread-safe and prevents concurrent execution.
     *
     * <p>The update process:
     * <ol>
     *   <li>Checks if state can be updated (started, not ended, not already updating)</li>
     *   <li>Checks if state is ready to end and not frozen</li>
     *   <li>If ready to end, calls {@link #end()}</li>
     *   <li>Otherwise, calls {@link #onUpdate()}</li>
     * </ol>
     *
     * <p>Any exceptions thrown during {@link #onUpdate()} are caught and logged.
     */
    public final void update() {
        stateLock.lock();
        try {
            if (!started || ended || updating) {
                return;
            }
            updating = true;
        } finally {
            stateLock.unlock();
        }

        try {
            if (isReadyToEnd() && !frozen) {
                end();
                return;
            }

            onUpdate();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Exception during " + getClass().getSimpleName() + " update");
        } finally {
            updating = false;
        }
    }

    /**
     * Hook method called during each update cycle.
     * Subclasses should override this to implement state-specific update logic.
     *
     * @throws Exception if an error occurs during state update
     */
    protected abstract void onUpdate() throws Exception;

    /**
     * Ends this state if it has been started and hasn't ended already.
     * This method is thread-safe and idempotent.
     *
     * <p>The end process:
     * <ol>
     *   <li>Checks if state can be ended (started and not already ended)</li>
     *   <li>Marks the state as ended</li>
     *   <li>Calls the {@link #onEnd()} hook</li>
     * </ol>
     *
     * <p>Any exceptions thrown during {@link #onEnd()} are caught and logged.
     */
    public final void end() {
        stateLock.lock();
        try {
            if (!started || ended) {
                return;
            }
            ended = true;
        } finally {
            stateLock.unlock();
        }

        try {
            onEnd();
            LOGGER.info(() -> "State " + getClass().getSimpleName() + " ended successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e, () -> "Exception during " + getClass().getSimpleName() + " end");
        }
    }

    /**
     * Hook method called when the state ends.
     * Subclasses should override this to implement state-specific cleanup logic.
     *
     * @throws Exception if an error occurs during state cleanup
     */
    protected abstract void onEnd() throws Exception;

    /**
     * Determines if this state is ready to end.
     *
     * <p>A state is ready to end if:
     * <ul>
     *   <li>It has already ended, OR</li>
     *   <li>Its remaining duration is zero or negative</li>
     * </ul>
     *
     * <p>Subclasses can override this method to provide custom end conditions.
     *
     * @return true if the state is ready to end, false otherwise
     */
    protected boolean isReadyToEnd() {
        return ended || getRemainingDuration().equals(Duration.ZERO);
    }

    /**
     * Gets the total duration this state should run for.
     *
     * @return the duration of this state
     */
    public abstract Duration getDuration();

    /**
     * Gets the remaining duration before this state should end.
     *
     * <p>This is calculated as the difference between the state's total duration
     * and the time elapsed since it started. If the calculated remaining time
     * is negative, returns {@link Duration#ZERO}.
     *
     * @return the remaining duration, or {@link Duration#ZERO} if time has expired
     * @throws IllegalStateException if the state hasn't been started yet
     */
    public final Duration getRemainingDuration() {
        if (!started) {
            throw new IllegalStateException("Cannot get remaining duration of unstarted state");
        }

        assert startInstant != null;
        Duration sinceStart = Duration.between(startInstant, Instant.now());
        Duration remaining = getDuration().minus(sinceStart);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Checks if this state has been started.
     *
     * @return true if the state has been started, false otherwise
     */
    public final boolean isStarted() {
        return started;
    }

    /**
     * Checks if this state has ended.
     *
     * @return true if the state has ended, false otherwise
     */
    public final boolean isEnded() {
        return ended;
    }

    /**
     * Checks if this state is frozen (prevented from ending automatically).
     *
     * @return true if the state is frozen, false otherwise
     */
    public final boolean isFrozen() {
        return frozen;
    }

    /**
     * Sets the frozen state of this state.
     * When frozen, the state will not end automatically even if its duration expires.
     *
     * @param frozen true to freeze the state, false to unfreeze it
     */
    public final void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    /**
     * Checks if this state is currently being updated.
     *
     * @return true if an update is in progress, false otherwise
     */
    public final boolean isUpdating() {
        return updating;
    }

    /**
     * Gets the instant when this state was started.
     *
     * @return the start instant
     * @throws IllegalStateException if the state hasn't been started yet
     */
    public final Instant getStartInstant() {
        if (!started) {
            throw new IllegalStateException("Cannot get start instant of unstarted state");
        }
        assert startInstant != null;
        return startInstant;
    }

    /**
     * Gets the elapsed time since this state started.
     *
     * @return the elapsed duration
     * @throws IllegalStateException if the state hasn't been started yet
     */
    public final Duration getElapsedDuration() {
        if (!started) {
            throw new IllegalStateException("Cannot get elapsed duration of unstarted state");
        }
        assert startInstant != null;
        return Duration.between(startInstant, Instant.now());
    }

    @Override
    public String toString() {
        return String.format("%s{started=%s, ended=%s, frozen=%s, updating=%s}",
                getClass().getSimpleName(), started, ended, frozen, updating);
    }
}