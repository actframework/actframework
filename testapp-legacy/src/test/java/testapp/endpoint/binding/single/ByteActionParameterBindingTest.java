package testapp.endpoint.binding.single;

public class ByteActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Byte> {
    @Override
    protected String primitiveUrlPath() {
        return "byte_p";
    }

    @Override
    protected String urlPath() {
        return "byte_w";
    }

    @Override
    protected Byte nonNullValue() {
        return 126;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0";
    }

    @Override
    protected Object outOfScopeValue() {
        return 999;
    }
}
