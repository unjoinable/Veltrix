package io.github.unjoinable.veltrix.fsm;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A StateHolder that executes its contained states sequentially, one after another.
 *
 * <p>StateSeries manages a sequence of states where only one state is active at a time.
 * When the current state is ready to end (or is skipped), the series automatically
 * transitions to the next state. The series ends when all states have completed.
 *
 * <p>Key features:
 * <ul>
 *   <li>Sequential execution - states run one after another</li>
 *   <li>Automatic transitions - no manual intervention needed</li>
 *   <li>Skip functionality - can force transition to next state</li>
 *   <li>Dynamic insertion - can add states at specific positions</li>
 * </ul>
 */
public class StateSeries extends StateHolder {

    private int current = 0;
    private boolean skipping = false;

    /**
     * Creates a new StateSeries with no initial states.
     */
    public StateSeries() {
        super();
    }

    /**
     * Creates a new StateSeries with the specified initial states.
     *
     * @param states the initial collection of states to execute sequentially
     */
    public StateSeries(Collection<State> states) {
        super(states);
    }

    /**
     * Creates a new StateSeries with the specified initial states.
     *
     * @param states the initial array of states to execute sequentially
     */
    public StateSeries(State... states) {
        super(Arrays.asList(states));
    }

    /**
     * Adds a state to be executed immediately after the current state.
     * If currently executing state 0, the new state will be inserted at position 1.
     *
     * @param state the state to add next in the sequence
     * @throws IllegalArgumentException if state is null
     * @throws IndexOutOfBoundsException if current position is invalid
     */
    public void addNext(State state) {
        if (current + 1 > states.size()) {
            states.add(state);
        } else {
            states.add(current + 1, state);
        }
    }

    /**
     * Adds multiple states to be executed immediately after the current state.
     * States will be inserted in the order provided.
     *
     * @param newStates the list of states to add next in the sequence
     * @throws IllegalArgumentException if newStates is null or contains null elements
     */
    public void addNext(List<State> newStates) {
        int insertPosition = current + 1;
        for (State state : newStates) {
            if (insertPosition > states.size()) {
                states.add(state);
            } else {
                states.add(insertPosition, state);
            }
            insertPosition++;
        }
    }

    /**
     * Forces the current state to end and immediately transition to the next state.
     * The current state will be ended even if it's not ready to end or is frozen.
     */
    public void skip() {
        skipping = true;
    }

    /**
     * Gets the index of the currently executing state.
     *
     * @return the current state index, or -1 if no states are present
     */
    public int getCurrentIndex() {
        return states.isEmpty() ? -1 : current;
    }

    /**
     * Gets the currently executing state.
     *
     * @return the current state, or null if no states are present or series has ended
     */
    public @Nullable State getCurrentState() {
        if (states.isEmpty() || current >= states.size()) {
            return null;
        }
        return states.get(current);
    }

    /**
     * Checks if there are more states to execute after the current one.
     *
     * @return true if there are more states, false otherwise
     */
    public boolean hasNext() {
        return current + 1 < states.size();
    }

    /**
     * Gets the number of remaining states (including the current one).
     *
     * @return the number of remaining states
     */
    public int getRemainingStates() {
        return Math.max(0, states.size() - current);
    }

    @Override
    protected void onStart() throws Exception {
        if (states.isEmpty()) {
            end();
            return;
        }

        states.get(current).start();
    }

    @Override
    protected void onUpdate() throws Exception {
        if (current >= states.size()) {
            return;
        }

        State currentState = states.get(current);
        currentState.update();

        if ((currentState.isReadyToEnd() && !currentState.isFrozen()) || skipping) {
            if (skipping) {
                skipping = false;
            }

            currentState.end();
            current++;

            if (current >= states.size()) {
                end();
                return;
            }

            states.get(current).start();
        }
    }

    @Override
    protected boolean isReadyToEnd() {
        if (states.isEmpty()) {
            return true;
        }
        return current == states.size() - 1 && states.get(current).isReadyToEnd();
    }

    @Override
    protected void onEnd()  {
        if (current < states.size()) {
            states.get(current).end();
        }
    }

    @Override
    public Duration getDuration() {
        return states.stream()
                .map(State::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    @Override
    public String toString() {
        return String.format("%s{current=%d/%d, skipping=%s, started=%s, ended=%s}",
                getClass().getSimpleName(), current, states.size(), skipping, isStarted(), isEnded());
    }
}