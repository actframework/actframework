package testapp.endpoint.binding.single;

public class IntActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Integer> {
    @Override
    protected String primitiveUrlPath() {
        return "int_p";
    }

    @Override
    protected String urlPath() {
        return "int_w";
    }

    @Override
    protected Integer nonNullValue() {
        return Integer.MIN_VALUE;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0";
    }

    @Override
    protected Object outOfScopeValue() {
        return Long.MAX_VALUE;
    }
}
