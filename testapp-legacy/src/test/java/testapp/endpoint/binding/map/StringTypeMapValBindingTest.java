package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class StringTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<String> {
    public StringTypeMapValBindingTest() {
        super("string_v", "string_k");
    }

    @Override
    public Map<String, String> nonEmptyMap() {
        return C.Map("a", "Abc", "b", "aBc", "c", "abC");
    }

}
