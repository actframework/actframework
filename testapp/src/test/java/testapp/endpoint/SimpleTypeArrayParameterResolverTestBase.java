package testapp.endpoint;

import org.junit.Before;
import org.junit.Test;
import org.osgl.util.C;
import testapp.endpoint.EndPointTestContext.RequestMethod;

import java.util.List;

public abstract class SimpleTypeArrayParameterResolverTestBase<T> extends ParameterResolverTestBase {

    protected static final String PARAM = "v";

    private String pathPrimitive;
    private String pathWrap;
    private String pathList;
    private String pathSet;
    private EndPointTestContext context;

    public SimpleTypeArrayParameterResolverTestBase() {
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

    protected abstract String expectedRespForNonEmptyList();

    protected abstract String expectedRespForNonEmptySet();

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
        context
                .expected(expected)
                .url(processUrl(urlPath))
                .params(listEncoding.encode(PARAM, data))
                .method(method)
                .applyTo(this);
    }

    /*
     * Test configuration items
     *
     *  1. primitive|wrap
     *  2. empty|non-empty
     *  3. get|post-form|post-json
     *  4. e-one|e-two|e-json
     */

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
