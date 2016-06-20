package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class IntArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Integer> {

    @Override
    protected String listPath() {
        return "int_list";
    }

    @Override
    protected String setPath() {
        return "int_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "int_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "int_pa";
    }

    @Override
    protected List<Integer> nonEmptyList() {
        return C.list(-1, 0, -1, 126);
    }

}
