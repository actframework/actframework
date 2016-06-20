package testapp.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.osgl.util.C;
import testapp.endpoint.EndPointTestContext.RequestMethod;

import java.util.List;
import java.util.TreeSet;

public abstract class SimpleTypeArrayActionParameterBindingTestBase<T> extends ActionParameterBindingTestBase {

    protected static final String PARAM = "v";

    private String pathPrimitive;
    private String pathWrap;
    private String pathList;
    private String pathSet;
    private EndPointTestContext context;

    public SimpleTypeArrayActionParameterBindingTestBase() {
        this.pathPrimitive = primitiveArrayPath();
        this.pathWrap = wrapperArrayPath();
        this.pathList = listPath();
        this.pathSet = setPath();
    }

    protected abstract String primitiveArrayPath();

    protected abstract String wrapperArrayPath();

    protected abstract String listPath();

    protected abstract String setPath();

    protected abstract List<T> nonEmptyList();

    protected String expectedRespForNonEmptyList() {
        return nonEmptyList().toString();
    }

    protected String expectedRespForNonEmptySet() {
        return new TreeSet<T>(nonEmptyList()).toString();
    }

    private String e() {
        return expectedRespForNonEmptyList();
    }

    private String es() {
        return expectedRespForNonEmptySet();
    }

    @Before
    public void initContext() {
        context = new EndPointTestContext();
    }

    @Override
    protected String urlContext() {
        return "/sapr";
    }

    private void _verify(String expected, String urlPath, List data, ListEncoding listEncoding, RequestMethod method) throws Exception {
        if (null == urlPath) {
            // for some type like String,
            // we don't have the primitive array endpoint path
            return;
        }
        context
                .expected(expected)
                .url(processUrl(urlPath))
                .params(listEncoding.encode(null == data ? "foo" : PARAM, null == data ? C.list() : data))
                .method(method)
                .applyTo(this);
    }

    /*
     * Test configuration items
     *
     *  1. primitive|wrap
     *  2. empty|non-empty|null
     *  3. get|post-form|post-json
     *  4. e-one|e-two|e-json
     */
    @Test
    public void testPrimitiveNullArrayGetEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNullArrayJSON() throws Exception {
        _verify("[]", pathPrimitive, null, ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveEmptyArrayJSON() throws Exception {
        _verify("[]", pathPrimitive, C.list(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayGetEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeOne() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testPrimitiveNonEmptyArrayFormDataEncodeJSON() throws Exception {
        _verify(e(), pathPrimitive, nonEmptyList(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    // ------------ Wrap array -------------

    @Test
    public void testWrapNullArrayGetEncodeOne() throws Exception {
        _verify("[]", pathWrap, null, ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathWrap, null, ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathWrap, null, ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathWrap, null, ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayJSON() throws Exception {
        _verify("[]", pathWrap, null, ListEncoding.JSON, RequestMethod.POST_JSON);
    }


    @Test
    public void testWrapEmptyArrayGetEncodeOne() throws Exception {
        _verify("[]", pathWrap, C.list(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathWrap, C.list(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathWrap, C.list(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathWrap, C.list(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayJSON() throws Exception {
        _verify("[]", pathWrap, C.list(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeOne() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeTwo() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeOne() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeJSON() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    // ------------ List -------------

    @Test
    public void testNullListGetEncodeOne() throws Exception {
        _verify("[]", pathList, null, ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNullListGetEncodeTwo() throws Exception {
        _verify("[]", pathList, null, ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNullListFormDataEncodeOne() throws Exception {
        _verify("[]", pathList, null, ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListFormDataEncodeTwo() throws Exception {
        _verify("[]", pathList, null, ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListJSON() throws Exception {
        _verify("[]", pathList, null, ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testEmptyListGetEncodeOne() throws Exception {
        _verify("[]", pathList, C.list(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testEmptyListGetEncodeTwo() throws Exception {
        _verify("[]", pathList, C.list(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testEmptyListFormDataEncodeOne() throws Exception {
        _verify("[]", pathList, C.list(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListFormDataEncodeTwo() throws Exception {
        _verify("[]", pathList, C.list(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListJSON() throws Exception {
        _verify("[]", pathList, C.list(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testNonEmptyListGetEncodeOne() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListGetEncodeTwo() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListFormDataEncodeOne() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeTwo() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeJSON() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    // ------------ Set -------------

    @Test
    public void testNullSetGetEncodeOne() throws Exception {
        _verify("[]", pathSet, null, ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNullSetGetEncodeTwo() throws Exception {
        _verify("[]", pathSet, null, ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNullSetFormDataEncodeOne() throws Exception {
        _verify("[]", pathSet, null, ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetFormDataEncodeTwo() throws Exception {
        _verify("[]", pathSet, null, ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetJSON() throws Exception {
        _verify("[]", pathSet, null, ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testEmptySetGetEncodeOne() throws Exception {
        _verify("[]", pathSet, C.list(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testEmptySetGetEncodeTwo() throws Exception {
        _verify("[]", pathSet, C.list(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testEmptySetFormDataEncodeOne() throws Exception {
        _verify("[]", pathSet, C.list(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetFormDataEncodeTwo() throws Exception {
        _verify("[]", pathSet, C.list(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetJSON() throws Exception {
        _verify("[]", pathSet, C.list(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testNonEmptySetGetEncodeOne() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ListEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetGetEncodeTwo() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ListEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetFormDataEncodeOne() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ListEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeTwo() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ListEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeJSON() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ListEncoding.JSON, RequestMethod.POST_JSON);
    }

}
