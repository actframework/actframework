package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.math.BigDecimal;
import java.util.List;

public class BigDecimalArrayActionParameterBindingTest extends SimpleTypeArrayActionParameterBindingTestBase<BigDecimal> {

    @Override
    protected String listPath() {
        return "b_dec_list";
    }

    @Override
    protected String setPath() {
        return "b_dec_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "b_dec_wa";
    }

    @Override
    protected List<BigDecimal> nonEmptyList() {
        return C.list(b(-1.03), b(0.0), b(-1.03), b(123421421.32342));
    }

    private BigDecimal b(double i) {
        return BigDecimal.valueOf(i);
    }
}
