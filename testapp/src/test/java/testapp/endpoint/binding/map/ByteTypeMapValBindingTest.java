package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class ByteTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Byte> {
    public ByteTypeMapValBindingTest() {
        super("bool_v", "bool_k");
    }

    @Override
    public Map<String, Byte> nonEmptyMap() {
        return C.map("a", 0, "b", 1, "c", 2);
    }

}
