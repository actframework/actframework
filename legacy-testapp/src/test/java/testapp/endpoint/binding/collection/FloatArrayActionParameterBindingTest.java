package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class FloatArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Float> {

    @Override
    protected String listPath() {
        return "float_list";
    }

    @Override
    protected String setPath() {
        return "float_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "float_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "float_pa";
    }

    @Override
    protected List<Float> nonEmptyList() {
        return C.list(-1.01F, 0.0F, -1.01F, Float.MAX_VALUE, Float.MIN_VALUE);
    }

}
