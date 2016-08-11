package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class FloatTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Float> {
    public FloatTypeMapValBindingTest() {
        super("float_v", "float_k");
    }

    @Override
    public Map<String, Float> nonEmptyMap() {
        return C.map("a", Float.MAX_VALUE, "b", Float.MIN_VALUE, "c", 0.02f);
    }

}
