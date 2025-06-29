package io.github.unjoinable.veltrix.fsm;

import java.util.*;

/**
 * Abstract base class for states that contain and manage a collection of child states.
 *
 * <p>StateHolder extends {@link State} and implements {@link Iterable} to provide
 * a container for managing multiple states as a single unit. All child states
 * will be affected by operations on the parent StateHolder (such as freezing).
 *
 * <p>This class is useful for implementing composite state patterns where multiple
 * states need to be managed together.
 */
public abstract class StateHolder extends State implements Iterable<State> {

    protected final List<State> states;

    /**
     * Creates a new StateHolder with no initial states.
     */
    protected StateHolder() {
        this(Collections.emptyList());
    }

    /**
     * Creates a new StateHolder with the specified initial states.
     *
     * @param states the initial collection of states to manage
     */
    protected StateHolder(Collection<State> states) {
        this.states = new ArrayList<>(states);
    }

    /**
     * Adds a single state to this holder.
     *
     * @param state the state to add
     * @throws IllegalArgumentException if state is null
     */
    public void add(State state) {
        states.add(state);
    }

    /**
     * Adds all states from the specified collection to this holder.
     *
     * @param newStates the collection of states to add
     * @throws IllegalArgumentException if newStates is null or contains null elements
     */
    public void addAll(Collection<State> newStates) {
        states.addAll(newStates);
    }

    /**
     * Removes a state from this holder.
     *
     * @param state the state to remove
     * @return true if the state was removed, false if it wasn't present
     */
    public boolean remove(State state) {
        return states.remove(state);
    }

    /**
     * Removes all states from this holder.
     */
    public void clear() {
        states.clear();
    }

    /**
     * Gets the number of states in this holder.
     *
     * @return the number of states
     */
    public int size() {
        return states.size();
    }

    /**
     * Checks if this holder contains no states.
     *
     * @return true if this holder is empty, false otherwise
     */
    public boolean isEmpty() {
        return states.isEmpty();
    }

    /**
     * Checks if this holder contains the specified state.
     *
     * @param state the state to check for
     * @return true if the state is present, false otherwise
     */
    public boolean contains(State state) {
        return states.contains(state);
    }

    /**
     * Gets an unmodifiable view of the states in this holder.
     *
     * @return an unmodifiable list of states
     */
    public List<State> getStates() {
        return Collections.unmodifiableList(states);
    }

    /**
     * Sets the frozen state of this holder and all its child states.
     * When frozen, neither this holder nor any of its child states will
     * end automatically even if their duration expires.
     *
     * @param frozen true to freeze all states, false to unfreeze them
     */
    public void setAllFrozen(boolean frozen) {
        for (State state : states) {
            state.setFrozen(frozen);
        }
        setFrozen(frozen);
    }

    /**
     * Returns an iterator over the states in this holder.
     *
     * @return an iterator for the contained states
     */
    @Override
    public Iterator<State> iterator() {
        return states.iterator();
    }

    /**
     * Returns a string representation of this StateHolder including
     * information about its child states.
     *
     * @return a string representation of this holder
     */
    @Override
    public String toString() {
        return String.format("%s{states=%d, started=%s, ended=%s, frozen=%s}",
                getClass().getSimpleName(), states.size(), isStarted(), isEnded(), isFrozen());
    }
}