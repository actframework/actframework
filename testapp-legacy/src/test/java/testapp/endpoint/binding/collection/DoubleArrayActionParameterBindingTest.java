package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.util.List;

public class DoubleArrayActionParameterBindingTest extends PrimitiveTypeArrayActionParameterBindingTestBase<Double> {

    @Override
    protected String listPath() {
        return "double_list";
    }

    @Override
    protected String setPath() {
        return "double_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "double_wa";
    }

    @Override
    protected String primitiveArrayPath() {
        return "double_pa";
    }

    @Override
    protected List<Double> nonEmptyList() {
        return C.list(-1.03D, 0D, -1.03D, Double.MAX_VALUE, Double.MIN_VALUE);
    }

}
