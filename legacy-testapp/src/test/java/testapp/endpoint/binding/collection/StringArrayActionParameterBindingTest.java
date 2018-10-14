package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class StringArrayActionParameterBindingTest extends SimpleTypeArrayActionParameterBindingTestBase<String> {

    @Override
    protected String listPath() {
        return "string_list";
    }

    @Override
    protected String setPath() {
        return "string_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "string_wa";
    }

    @Override
    protected List<String> nonEmptyList() {
        return C.list("123", "", "foobar", "\n", "a\n\rb");
    }

}
