package act.metric;

/**
 * Implement a do-nothing {@link Metric}
 */
enum NullMetric implements Metric {
    INSTANCE
    ;

    private static final Timer NULL_TIMER = new Timer() {
        @Override
        public String name() {
            return null;
        }

        @Override
        public void stop() {
        }

        @Override
        public long ns() {
            return 0;
        }
    };

    @Override
    public void countOnce(String name) {
    }

    @Override
    public Timer startTimer(String name) {
        return NULL_TIMER;
    }

}
