package testapp.endpoint.binding.single;

import java.math.BigInteger;

public class LongActionParameterBindingTest extends PrimitiveTypeActionParameterBindingTestBase<Long> {
    @Override
    protected String primitiveUrlPath() {
        return "long_p";
    }

    @Override
    protected String urlPath() {
        return "long_w";
    }

    @Override
    protected Long nonNullValue() {
        return Long.MIN_VALUE;
    }

    @Override
    protected String primitiveDefValueStr() {
        return "0";
    }

    @Override
    protected Object outOfScopeValue() {
        BigInteger I = BigInteger.valueOf(Long.MAX_VALUE);
        I = I.add(BigInteger.ONE);
        return I;
    }
}
