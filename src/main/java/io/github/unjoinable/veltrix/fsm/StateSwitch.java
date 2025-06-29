package io.github.unjoinable.veltrix.fsm;

import org.jspecify.annotations.Nullable;

/**
 * A state machine implementation that manages transitions between different states.
 * This class provides a simple finite state machine where only one state can be active at a time.
 *
 * <p>The StateSwitch handles the lifecycle of states by properly ending the current state
 * before transitioning to a new one, ensuring clean state transitions.</p>
 */
public class StateSwitch {
    protected @Nullable State state;

    /**
     * Changes the current state to a new state.
     *
     * <p>This method performs a safe state transition by:</p>
     * <ul>
     *   <li>Calling {@link State#end()} on the current state (if one exists)</li>
     *   <li>Setting the new state as the current state</li>
     *   <li>Calling {@link State#start()} on the new state</li>
     * </ul>
     *
     * @param next the new state to transition to. Must not be null.
     */
    public void changeState(State next) {
        if (state != null) {
            state.end();
        }
        state = next;
        next.start();
    }

    /**
     * Updates the current state by calling its update method.
     *
     * <p>This method should typically be called in the main game/application loop
     * to allow the current state to perform its per-frame logic.</p>
     *
     * <p>If no state is currently active, this method does nothing.</p>
     */
    public void update() {
        if (state != null) {
            state.update();
        }
    }

    /**
     * Gets the currently active state.
     *
     * @return the current state, or null if no state is active
     */
    public @Nullable State getCurrentState() {
        return state;
    }

    /**
     * Checks if a state is currently active.
     *
     * @return true if a state is currently active, false otherwise
     */
    public boolean hasActiveState() {
        return state != null;
    }
}