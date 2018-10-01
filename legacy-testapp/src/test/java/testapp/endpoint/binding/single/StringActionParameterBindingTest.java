package testapp.endpoint.binding.single;

public class StringActionParameterBindingTest extends SimpleTypeActionParameterBindingTestBase<String> {

    @Override
    protected String urlPath() {
        return "string";
    }

    @Override
    protected String nonNullValue() {
        return "hello world";
    }

}
