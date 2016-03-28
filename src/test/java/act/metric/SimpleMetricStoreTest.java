package act.metric;

import act.TestBase;
import org.junit.Before;
import org.junit.Test;

public class SimpleMetricStoreTest extends TestBase {
    private SimpleMetricStore store;

    @Before
    public void prepare() {
        store = new SimpleMetricStore();
    }

    @Test
    public void countOnceShallIncreaseCounterByOne() {
        store.countOnce("abc");
        eq(1L, store.count("abc"));
        store.countOnce("abc");
        eq(2L, store.count("abc"));
    }

    @Test
    public void countOnceShallAggregateToParentCounter() {
        store.countOnce("a:b:c");
        store.countOnce("a:x:y");
        store.countOnce("a:b:d");
        store.countOnce("a:x:z");
        store.countOnce("abc");
        eq(1L, store.count("a:b:c"));
        eq(2L, store.count("a:b"));
        eq(1L, store.count("a:x:z"));
        eq(2L, store.count("a:x"));
        eq(4L, store.count("a"));
    }
}
