package testapp.endpoint;

import org.osgl.http.H;
import org.osgl.util.C;

import java.util.List;

public class EnumArrayActionParameterBindingTest extends SimpleTypeArrayActionParameterBindingTestBase<H.Method> {

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
    protected String primitiveArrayPath() {
        return null;
    }

    @Override
    protected List<H.Method> nonEmptyList() {
        return C.list(H.Method.GET, H.Method.POST, H.Method.CONNECT);
    }

}
