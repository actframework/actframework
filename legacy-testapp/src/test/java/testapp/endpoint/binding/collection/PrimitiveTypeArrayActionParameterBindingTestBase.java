package testapp.endpoint.binding.collection;

import org.junit.Test;
import org.osgl.util.C;
import testapp.endpoint.EndPointTestContext;
import testapp.endpoint.ParamEncoding;

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
        _verify("[]", pathPrimitive, null, ParamEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, null, ParamEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayJSON() throws Exception {
        if (this instanceof CharArrayActionParameterBindingTest) {
            // TODO: track fastjson #821
            // ignore see https://github.com/alibaba/fastjson/issues/821
            return;
        }
        _verify("[]", pathPrimitive, null, ParamEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.THREE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayJSON() throws Exception {
        // TODO: track FastJson issue #821
        if (this instanceof CharArrayActionParameterBindingTest) {
            // ignore. see https://github.com/alibaba/fastjson/issues/821
            return;
        }
        _verify("[]", pathPrimitive, C.list(), ParamEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.ONE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.TWO, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeThree() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.THREE, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeFour() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.FOUR, EndPointTestContext.RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.ONE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.TWO, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeThree() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.THREE, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeFour() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.FOUR, EndPointTestContext.RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeJSON() throws Exception {
        if (this instanceof CharArrayActionParameterBindingTest) {
            //TODO track fastjson issue #821
            //Ignore //see https://github.com/alibaba/fastjson/issues/821
            return;
        }
        _verify(e(), pathPrimitive, nonEmptyList(), ParamEncoding.JSON, EndPointTestContext.RequestMethod.POST_JSON);
    }

}
