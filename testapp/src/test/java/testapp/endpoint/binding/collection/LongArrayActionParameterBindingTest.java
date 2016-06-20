package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class LongArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Long> {

    @Override
    protected String listPath() {
        return "long_list";
    }

    @Override
    protected String setPath() {
        return "long_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "long_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "long_pa";
    }

    @Override
    protected List<Long> nonEmptyList() {
        return C.list(-1L, 0L, -1L, Long.MAX_VALUE, Long.MIN_VALUE);
    }

}
