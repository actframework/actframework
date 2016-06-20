package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class BooleanArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Boolean> {

    @Override
    protected String listPath() {
        return "bool_list";
    }

    @Override
    protected String setPath() {
        return "bool_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "bool_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "bool_pa";
    }

    @Override
    protected List<Boolean> nonEmptyList() {
        return C.list(true, false, true, true);
    }
}
