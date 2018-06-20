package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class ShortTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Short> {
    public ShortTypeMapValBindingTest() {
        super("short_v", "short_k");
    }

    @Override
    public Map<String, Short> nonEmptyMap() {
        return C.Map("a", Short.MAX_VALUE, "b", Short.MIN_VALUE, "c", (short)0);
    }

}
