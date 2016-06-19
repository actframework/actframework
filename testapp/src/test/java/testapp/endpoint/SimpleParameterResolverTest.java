package testapp.endpoint;

import org.junit.Test;
import org.osgl.$;
import org.osgl.util.C;
import org.osgl.util.S;
import testapp.EndpointTester;

import java.lang.annotation.ElementType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class SimpleParameterResolverTest extends ParameterResolverTestBase {

    private static final String PARAM = "v";

    @Override
    protected String urlContext() {
        return "/spr";
    }

    private void doVerify(boolean primitive, String url, Object min, Object max, Object outOfScope, String encodedVal, String defValStr) throws Exception {
        verify(S.string(min), url, PARAM, min);
        verify(encodedVal, url, PARAM, encodedVal);
        verify(S.string(max), url, PARAM, max);
        if (null != outOfScope) {
            badRequest(url, PARAM, outOfScope);
        }
        if (primitive) {
            verify(defValStr, url, "foo", "bar");
        } else {
            notFound(url, "foo", "bar");
        }
    }

    @Test
    public void testRequired() throws Exception {
        badRequest("v_is_required", "foo", "bar");
    }

    @Test
    public void boolP() throws Exception {
        final String url = "bool_p";
        doVerify(true, url, false, true, null, "true", "false");
        verify("false", url, PARAM, "yes");
        verify("false", url, PARAM, "no");
    }

    @Test
    public void takeBool() throws Exception {
        final String url = "bool";
        doVerify(false, url, false, true, null, "true", "false");
        verify("false", url, PARAM, "yes");
        verify("false", url, PARAM, "no");
    }

    @Test
    public void byteP() throws Exception {
        final String url = "byte_p";
        doVerify(true, url, Byte.MIN_VALUE, Byte.MAX_VALUE, Integer.MAX_VALUE, "64", "0");
        badRequest(url, PARAM, "not-a-byte");
    }

    @Test
    public void takeByte() throws Exception {
        final String url = "byte";
        doVerify(false, url, Byte.MIN_VALUE, Byte.MAX_VALUE, Integer.MAX_VALUE, "64", "0");
        badRequest(url, PARAM, "not-a-byte");
    }

    @Test
    public void shortP() throws Exception {
        final String url = "short_p";
        doVerify(true, url, Short.MIN_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, "3721", "0");
        badRequest(url, PARAM, "not-a-short");
    }

    @Test
    public void takeShort() throws Exception {
        final String url = "byte";
        doVerify(false, url, Short.MIN_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, "3721", "0");
        badRequest(url, PARAM, "not-a-short");
    }

    @Test
    public void charP() throws Exception {
        final String url = "char_p";
        doVerify(true, url, Character.MIN_VALUE, Character.MAX_VALUE, null, "中", S.string('\0'));
    }

    @Test
    public void takeChar() throws Exception {
        final String url = "char";
        doVerify(false, url, Character.MIN_VALUE, Character.MAX_VALUE, null, "中", S.string('\0'));
    }

    @Test
    public void intP() throws Exception {
        final String url = "int_p";
        doVerify(true, url, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, "37", "0");
    }

    @Test
    public void takeInt() throws Exception {
        final String url = "int";
        doVerify(false, url, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, "37", "0");
    }

    @Test
    public void floatP() throws Exception {
        final String url = "float_p";
        doVerify(true, url, Float.MIN_VALUE, Float.MAX_VALUE, Long.MAX_VALUE, "37.3", "0");
    }

    @Test
    public void takeFloat() throws Exception {
        final String url = "float";
        doVerify(false, url, Float.MIN_VALUE, Float.MAX_VALUE, Long.MAX_VALUE, "37.3", "0");
    }

    @Test
    public void longP() throws Exception {
        final String url = "long_p";
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger outOfScope = maxLong.add(BigInteger.ONE);
        doVerify(true, url, Long.MIN_VALUE, Long.MAX_VALUE, outOfScope, "73213457", "0");
    }

    @Test
    public void takeLong() throws Exception {
        final String url = "long";
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger outOfScope = maxLong.add(BigInteger.ONE);
        doVerify(false, url, Long.MIN_VALUE, Long.MAX_VALUE, outOfScope, "73213457", "0");
    }

    @Test
    public void bigInteger() throws Exception {
        final String url = processUrl("b_int");
        BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
        verifyAllMethods(S.string(maxLong), url, PARAM, maxLong);
        notFoundByAllMethods(url, "foo", "bar");
    }

    @Test
    public void bigDecimal() throws Exception {
        final String url = processUrl("b_dec");
        BigDecimal bd = new BigDecimal("2.33");
        verifyAllMethods(S.string(bd), url, PARAM, bd);
        notFoundByAllMethods(url, "foo", "bar");
    }

    @Test
    public void string() throws Exception {
        final String url = processUrl("string");
        final String s = S.random(100);
        verifyAllMethods(s, url, PARAM, s);
        notFoundByAllMethods(url, "foo", "bar");
    }

    @Test
    public void takeEnum() throws Exception {
        final String url = processUrl("enum");
        ElementType type = $.random(C.listOf(ElementType.values()));
        verifyAllMethods(type.toString(), url, PARAM, type);
        notFoundByAllMethods(url, "foo", "bar");
    }

}
