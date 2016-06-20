package testapp.endpoint.binding.collection;

import org.osgl.util.C;
import testapp.model.RGB;

import java.util.List;

public class EnumArrayActionParameterBindingTest extends SimpleTypeArrayActionParameterBindingTestBase<RGB> {

    @Override
    protected String listPath() {
        return "enum_list";
    }

    @Override
    protected String setPath() {
        return "enum_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "enum_wa";
    }

    @Override
    protected List<RGB> nonEmptyList() {
        return C.list(RGB.R, RGB.B, RGB.G, RGB.R);
    }

}
