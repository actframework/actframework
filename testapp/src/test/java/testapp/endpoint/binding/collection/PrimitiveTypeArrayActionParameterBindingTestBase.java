package testapp.endpoint.binding.collection;

import org.junit.Test;
import org.osgl.util.C;
import testapp.endpoint.EndPointTestContext;
import testapp.endpoint.ListEncoding;

/**
 * Test case base class for primitive type and their wrapper classes
 */
public abstract class PrimitiveTypeArrayActionParameterBindingTestBase<T> extends SimpleTypeArrayActionParameterBindingTestBase<T> {
    private String pathPrimitive;

    protected abstract String primitiveArrayPath();

    public PrimitiveTypeArrayActionParameterBindingTestBase() {
        pathPrimitive = primitiveArrayPath();
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayJSON() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.THREE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayJSON() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeThree() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeFour() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeThree() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.THREE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeFour() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeJSON() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

}
