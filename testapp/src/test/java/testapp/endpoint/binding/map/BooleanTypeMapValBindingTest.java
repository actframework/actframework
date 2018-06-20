package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class BooleanTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Boolean> {
    public BooleanTypeMapValBindingTest() {
        super("bool_v", "bool_k");
    }

    @Override
    public Map<String, Boolean> nonEmptyMap() {
        return C.Map("a", true, "b", false, "c", true);
    }

}
