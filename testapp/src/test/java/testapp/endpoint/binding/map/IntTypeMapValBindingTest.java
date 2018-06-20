package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class IntTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Integer> {
    public IntTypeMapValBindingTest() {
        super("int_v", "int_k");
    }

    @Override
    public Map<String, Integer> nonEmptyMap() {
        return C.Map("a", Integer.MAX_VALUE, "b", Integer.MIN_VALUE, "c", 0);
    }

}
