package testapp.endpoint.binding.single;

public class FloatActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Float> {
    @Override
    protected String primitiveUrlPath() {
        return "float_p";
    }

    @Override
    protected String urlPath() {
        return "float_w";
    }

    @Override
    protected Float nonNullValue() {
        return Float.MIN_VALUE;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0.0";
    }

    @Override
    protected Object outOfScopeValue() {
        return Double.MAX_VALUE;
    }

}