package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class ByteArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Byte> {

    @Override
    protected String listPath() {
        return "byte_list";
    }

    @Override
    protected String setPath() {
        return "byte_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "byte_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "byte_pa";
    }

    @Override
    protected List<Byte> nonEmptyList() {
        return C.list(b(-1), b(0), b(-1), b(126));
    }

    private byte b(int i) {
        return (byte) i;
    }

}
