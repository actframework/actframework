package testapp.endpoint.binding.single;

public class CharActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Character> {
    @Override
    protected String primitiveUrlPath() {
        return "char_p";
    }

    @Override
    protected String urlPath() {
        return "char_w";
    }

    @Override
    protected Character nonNullValue() {
        return '中';
    }

    @Override
    protected String primitiveDefValueStr() {
        return "\0";
    }

    @Override
    protected Object outOfScopeValue() {
        return Integer.MAX_VALUE;
    }
}
