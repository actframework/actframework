package testapp.endpoint.binding.single;

import testapp.model.RGB;

public class EnumActionParameterBindingTest extends SimpleTypeActionParameterBindingTestBase<RGB> {

    @Override
    protected String urlPath() {
        return "enum";
    }

    @Override
    protected RGB nonNullValue() {
        return RGB.R;
    }

}
