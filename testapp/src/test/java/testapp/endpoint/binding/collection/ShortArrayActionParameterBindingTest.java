package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class ShortArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Short> {

    @Override
    protected String listPath() {
        return "short_list";
    }

    @Override
    protected String setPath() {
        return "short_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "short_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "short_pa";
    }

    @Override
    protected List<Short> nonEmptyList() {
        return C.list(x(-1), x(0), x(-1), x(126));
    }

    private short x(int i) {
        return (short) i;
    }
}
