package testapp.endpoint.binding.map;

import org.osgl.util.C;
import testapp.model.RGB;

import java.util.Map;

public class EnumTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<RGB> {
    public EnumTypeMapValBindingTest() {
        super("enum_v", "enum_k");
    }

    @Override
    public Map<String, RGB> nonEmptyMap() {
        return C.Map("a", RGB.R, "b", RGB.G, "c", RGB.B);
    }

}
