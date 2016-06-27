package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class DoubleTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Double> {
    public DoubleTypeMapValBindingTest() {
        super("double_v", "double_k");
    }

    @Override
    public Map<String, Double> nonEmptyMap() {
        return C.map("a", Double.MAX_VALUE, "b", Double.MIN_VALUE, "c", 0.02);
    }

}
