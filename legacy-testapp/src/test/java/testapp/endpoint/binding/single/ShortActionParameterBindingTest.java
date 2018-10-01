package testapp.endpoint.binding.single;

public class ShortActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Short> {
    @Override
    protected String primitiveUrlPath() {
        return "short_p";
    }

    @Override
    protected String urlPath() {
        return "short_w";
    }

    @Override
    protected Short nonNullValue() {
        return Short.MIN_VALUE;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0";
    }

    @Override
    protected Object outOfScopeValue() {
        return Integer.MAX_VALUE;
    }
}
