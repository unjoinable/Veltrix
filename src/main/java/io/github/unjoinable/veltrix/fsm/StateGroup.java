package io.github.unjoinable.veltrix.fsm;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A state that manages multiple child states concurrently, running them all in parallel.
 *
 * <p>StateGroup executes all its child states simultaneously and only completes when
 * all child states have ended. This is useful for:</p>
 * <ul>
 *   <li>Running multiple independent states at the same time</li>
 *   <li>Coordinating parallel operations that must all complete</li>
 *   <li>Creating composite behaviors from simpler states</li>
 *   <li>Synchronizing multiple state machines</li>
 * </ul>
 *
 * <p>The group's duration is determined by the longest-running child state,
 * and the group is only ready to end when all child states are ready to end.</p>
 */
public class StateGroup extends StateHolder {

    /**
     * Constructs a new StateGroup with the specified list of child states.
     *
     * @param states the list of states to run concurrently. Defaults to empty list if null.
     */
    public StateGroup(List<State> states) {
        super(states);
    }

    /**
     * Constructs a new StateGroup with no initial states.
     */
    public StateGroup() {
        this(Collections.emptyList());
    }

    /**
     * Constructs a new StateGroup with the specified child states.
     *
     * @param states the states to run concurrently
     */
    public StateGroup(State... states) {
        this(Arrays.asList(states));
    }

    /**
     * Starts all child states when the group starts.
     *
     * <p>This method calls {@link State#start()} on each child state,
     * allowing them to begin their execution concurrently.</p>
     */
    @Override
    protected void onStart() {
        for (State state : states) {
            state.start();
        }
    }

    /**
     * Updates all child states and checks if the group should end.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Calls {@link State#update()} on each child state</li>
     *   <li>Checks if all child states have ended</li>
     *   <li>If all child states have ended, ends this group</li>
     * </ul>
     */
    @Override
    protected void onUpdate() {
        for (State state : states) {
            state.update();
        }

        // Check if all child states have ended
        boolean allEnded = true;
        for (State state : states) {
            if (!state.isEnded()) {
                allEnded = false;
                break;
            }
        }

        if (allEnded) {
            end();
        }
    }

    /**
     * Ends all child states when the group ends.
     *
     * <p>This method calls {@link State#end()} on each child state,
     * ensuring proper cleanup of all child states.</p>
     */
    @Override
    protected void onEnd() {
        for (State state : states) {
            state.end();
        }
    }

    /**
     * Checks if the group is ready to end by verifying all child states are ready.
     *
     * <p>The group is only ready to end when all of its child states are ready to end.
     * This ensures that all parallel operations can complete gracefully.</p>
     *
     * @return true if all child states are ready to end, false otherwise
     */
    @Override
    public boolean isReadyToEnd() {
        for (State state : states) {
            if (!state.isReadyToEnd()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the duration of the longest-running child state.
     *
     * <p>Since all child states run concurrently, the group's total duration
     * is determined by whichever child state takes the longest to complete.
     * If there are no child states, returns Duration.ZERO.</p>
     *
     * @return the maximum duration among all child states, or Duration.ZERO if no states
     */
    @Override
    public Duration getDuration() {
        Duration maxDuration = Duration.ZERO;
        for (State state : states) {
            Duration stateDuration = state.getDuration();
            if (stateDuration.compareTo(maxDuration) > 0) {
                maxDuration = stateDuration;
            }
        }
        return maxDuration;
    }
}