package testapp.endpoint.binding.map;

import org.osgl.util.C;

import java.util.Map;

public class ByteTypeMapValBindingTest extends SimpleTypeMapValBindingTestBase<Byte> {
    public ByteTypeMapValBindingTest() {
        super("byte_v", "byte_k");
    }

    @Override
    public Map<String, Byte> nonEmptyMap() {
        return C.Map("a", 0, "b", 1, "c", 2);
    }

}
