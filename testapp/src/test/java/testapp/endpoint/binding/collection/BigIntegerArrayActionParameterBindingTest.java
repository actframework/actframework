package testapp.endpoint.binding.collection;

import org.osgl.util.C;

import java.math.BigInteger;
import java.util.List;

public class BigIntegerArrayActionParameterBindingTest extends SimpleTypeArrayActionParameterBindingTestBase<BigInteger> {

    @Override
    protected String listPath() {
        return "b_int_list";
    }

    @Override
    protected String setPath() {
        return "b_int_set";
    }

    @Override
    protected String wrapperArrayPath() {
        return "b_int_wa";
    }

    @Override
    protected List<BigInteger> nonEmptyList() {
        return C.list(b(-1), b(0), b(-1), b(123421421));
    }

    private BigInteger b(int i) {
        return BigInteger.valueOf((long) i);
    }
}
