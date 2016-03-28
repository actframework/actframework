package act.metric;

/**
 * A Timer that could be used to measure execution duration of a certain process
 */
public interface Timer {

    /**
     * Returns the name of the timer. The string returned should be
     * identical to the string passed in {@link Metric#startTimer(String)}
     * method that returns this timer
     *
     * @return the timer's name
     */
    String name();

    /**
     * Stop the timer
     */
    void stop();

    /**
     * Returns nanoseconds the time has elapsed since time being created till
     * {@link #stop()} method get called
     * @return the duration time in nanoseconds
     */
    long ns();

}
