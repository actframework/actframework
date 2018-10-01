package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class LongTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Long> {
    public LongTypeMapValBindingTest() {
        super("long_v", "long_k");
    }

    @Override
    public Map<String, Long> nonEmptyMap() {
        return C.Map("a", Long.MAX_VALUE, "b", Long.MIN_VALUE, "c", 0L);
    }

}
