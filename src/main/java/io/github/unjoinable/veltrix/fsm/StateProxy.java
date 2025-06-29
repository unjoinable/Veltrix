package io.github.unjoinable.veltrix.fsm;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Abstract base class for proxy states that delegate their behavior to a series of other states.
 *
 * <p>StateProxy is a special type of state that doesn't perform any direct logic itself,
 * but instead creates and adds a sequence of other states to a StateSeries when it starts.
 * This pattern is useful for:</p>
 * <ul>
 *   <li>Grouping related states together</li>
 *   <li>Creating reusable state sequences</li>
 *   <li>Implementing composite state patterns</li>
 *   <li>Dynamically generating state sequences based on runtime conditions</li>
 * </ul>
 *
 * <p>The proxy state itself has zero duration and performs no updates or cleanup,
 * as its sole purpose is to inject states into the series during initialization.</p>
 */
public abstract class StateProxy extends State {
    private final StateSeries series;

    /**
     * Constructs a new StateProxy with the specified state series.
     *
     * @param series the StateSeries that will receive the created states. Must not be null.
     */
    protected StateProxy(StateSeries series) {
        this.series = series;
    }

    /**
     * Called when this proxy state starts. Automatically creates and adds the proxy states
     * to the associated series.
     *
     * <p>This method calls {@link #createStates()} to get the list of states to add,
     * then adds them all to the series using {@link StateSeries#addNext(List)}.</p>
     */
    @Override
    protected void onStart() {
        series.addNext(createStates());
    }

    /**
     * Creates the list of states that this proxy should add to the series.
     *
     * <p>This method is called during {@link #onStart()} and should return
     * a list of states that will be added to the StateSeries. The states will
     * be executed in the order they appear in the list.</p>
     *
     * @return a list of states to add to the series.
     */
    public abstract List<State> createStates();

    /**
     * No-op update method since proxy states don't perform any logic themselves.
     * The actual work is done by the states added to the series.
     */
    @Override
    protected void onUpdate() {
        // Proxy states don't update - they delegate to the states they create
    }

    /**
     * No-op end method since proxy states don't need cleanup.
     * Any cleanup should be handled by the individual states added to the series.
     */
    @Override
    protected void onEnd() {
        // Proxy states don't need cleanup - they delegate to the states they create
    }

    /**
     * Returns zero duration since proxy states are instantaneous.
     * They exist only to add other states to the series and then complete immediately.
     *
     * @return Duration.ZERO indicating this state completes instantly
     */
    @Override
    public Duration getDuration() {
        return Duration.ZERO;
    }

    /**
     * Factory method to create a StateProxy using a lambda function.
     *
     * <p>This is a convenience method that allows creating proxy states without
     * having to extend the StateProxy class. Instead, you can provide a lambda
     * or method reference that returns the list of states.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * StateProxy proxy = StateProxy.create(series, () -> Arrays.asList(
     *     new SomeState(),
     *     new AnotherState(),
     *     new FinalState()
     * ));
     * }</pre>
     *
     * @param series the StateSeries to add states to
     * @param stateSupplier a supplier function that returns the list of states to create
     * @return a new StateProxy instance that will use the provided supplier
     * @throws NullPointerException if series or stateSupplier is null
     */
    public static StateProxy create(StateSeries series, Supplier<List<State>> stateSupplier) {
        return new LambdaStateProxy(series, stateSupplier);
    }
}

/**
 * Implementation of StateProxy that uses a Supplier function to create states.
 *
 * <p>This class is typically not used directly - instead use the factory method
 * {@link StateProxy#create(StateSeries, Supplier)} which returns an instance of this class.</p>
 */
class LambdaStateProxy extends StateProxy {
    private final Supplier<List<State>> stateSupplier;

    /**
     * Constructs a new LambdaStateProxy with the specified series and state supplier.
     *
     * @param series the StateSeries to add states to
     * @param stateSupplier the supplier function that creates states
     * @throws NullPointerException if series or stateSupplier is null
     */
    LambdaStateProxy(StateSeries series, Supplier<List<State>> stateSupplier) {
        super(series);
        this.stateSupplier = stateSupplier;
    }

    /**
     * Creates states by calling the provided supplier function.
     *
     * @return the list of states returned by the supplier function
     */
    @Override
    public List<State> createStates() {
        return stateSupplier.get();
    }
}