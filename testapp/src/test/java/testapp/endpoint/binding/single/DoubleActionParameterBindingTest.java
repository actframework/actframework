package testapp.endpoint.binding.single;

import java.math.BigDecimal;

public class DoubleActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Double> {
    @Override
    protected String primitiveUrlPath() {
        return "double_p";
    }

    @Override
    protected String urlPath() {
        return "double_w";
    }

    @Override
    protected Double nonNullValue() {
        return Double.MIN_VALUE;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0.0";
    }

    @Override
    protected Object outOfScopeValue() {
        BigDecimal dec = BigDecimal.valueOf(Double.MAX_VALUE);
        dec = dec.add(dec);
        return dec;
    }
}
