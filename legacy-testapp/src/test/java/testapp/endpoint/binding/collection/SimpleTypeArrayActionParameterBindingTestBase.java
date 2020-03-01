package testapp.endpoint.binding.collection;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;
import org.osgl.util.Generics;
import testapp.endpoint.EndPointTestContext.RequestMethod;
import testapp.endpoint.ParamEncoding;
import testapp.endpoint.binding.ActionParameterBindingTestBase;

import java.math.BigDecimal;
import java.util.*;

public abstract class SimpleTypeArrayActionParameterBindingTestBase<T> extends ActionParameterBindingTestBase {

    protected static final String PARAM = "v";

    private static Class targetType;
    private boolean isNumber;

    private String pathWrap;
    private String pathList;
    private String pathSet;

    public SimpleTypeArrayActionParameterBindingTestBase() {
        this.pathWrap = wrapperArrayPath();
        this.pathList = listPath();
        this.pathSet = setPath();
        this.targetType = (Class<T>) Generics.typeParamImplementations(getClass(), SimpleTypeArrayActionParameterBindingTestBase.class).get(0);
        this.isNumber = Number.class.isAssignableFrom(this.targetType);
    }


    protected abstract String wrapperArrayPath();

    protected abstract String listPath();

    protected abstract String setPath();

    protected abstract List<T> nonEmptyList();

    protected String expectedRespForNonEmptyList() {
        return JSON.toJSONString(nonEmptyList());
    }

    protected String expectedRespForNonEmptySet() {
        return JSON.toJSONString(new TreeSet(nonEmptyList()));
    }

    protected final String e() {
        return expectedRespForNonEmptyList();
    }

    protected final String e2() {
        if (isNumber) {
            List<T> l = nonEmptyList();
            List<BigDecimal> l2 = new ArrayList<>();
            for (T t: l) {
                l2.add(BigDecimal.valueOf(((Number) t).doubleValue()));
            }
            return l2.toString();
        }
        return null;
    }

    private String es() {
        return expectedRespForNonEmptySet();
    }

    private String es2() {
        if (isNumber) {
            List<T> l = nonEmptyList();
            Set<BigDecimal> l2 = new TreeSet<>();
            for (T t: l) {
                l2.add(BigDecimal.valueOf(((Number) t).doubleValue()));
            }
            return l2.toString();
        }
        return null;
    }

    @Override
    protected String urlContext() {
        return "/sapr";
    }

    protected final void _verify(String expected, String urlPath, List data, ParamEncoding paramEncoding, RequestMethod method) throws Exception {
        context
                .expected(expected, e2(), es2())
                .accept(H.Format.JSON)
                .url(processUrl(urlPath))
                .params(paramEncoding.encode(null == data ? "v" : PARAM, null == data ? C.list() : data))
                .method(method)
                .applyTo(this);
    }

    /*
     * Test configuration items
     *
     *  1. primitive|wrap
     *  2. empty|non-empty|null
     *  3. get|post-form|post-json
     *  4. e-one|e-two|e-three|e-four|e-json
     */
    // ------------ Wrap array -------------

    @Test
    public void testWrapNullArrayGetEncodeOne() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayGetEncodeThree() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayGetEncodeFour() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNullArrayJSON() throws Exception {
        _verify("[]", pathWrap, null, ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testWrapEmptyArrayGetEncodeOne() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayGetEncodeTwo() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayGetEncodeThree() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayGetEncodeFour() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeOne() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeThree() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayFormDataEncodeFour() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapEmptyArrayJSON() throws Exception {
        _verify("[]", pathWrap, C.list(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeOne() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeTwo() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeThree() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayGetEncodeFour() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeOne() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeTwo() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeThree() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeFour() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testWrapNonEmptyArrayFormDataEncodeJSON() throws Exception {
        _verify(e(), pathWrap, nonEmptyList(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    // ------------ List -------------

    @Test
    public void testNullListGetEncodeOne() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNullListGetEncodeTwo() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNullListGetEncodeThree() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testNullListGetEncodeFour() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testNullListFormDataEncodeOne() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListFormDataEncodeTwo() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListFormDataEncodeThree() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListFormDataEncodeFour() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullListJSON() throws Exception {
        _verify("[]", pathList, null, ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testEmptyListGetEncodeOne() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testEmptyListGetEncodeTwo() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testEmptyListGetEncodeThree() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testEmptyListGetEncodeFour() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testEmptyListFormDataEncodeOne() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListFormDataEncodeTwo() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListFormDataEncodeThree() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListFormDataEncodeFour() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptyListJSON() throws Exception {
        _verify("[]", pathList, C.list(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testNonEmptyListGetEncodeOne() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListGetEncodeTwo() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListGetEncodeThree() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListGetEncodeFour() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testNonEmptyListFormDataEncodeOne() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeTwo() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeThree() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeFour() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptyListFormDataEncodeJSON() throws Exception {
        _verify(e(), pathList, nonEmptyList(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    // ------------ Set -------------

    @Test
    public void testNullSetGetEncodeOne() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNullSetGetEncodeTwo() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNullSetGetEncodeThree() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testNullSetGetEncodeFour() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testNullSetFormDataEncodeOne() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetFormDataEncodeTwo() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetFormDataEncodeThree() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetFormDataEncodeFour() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNullSetJSON() throws Exception {
        _verify("[]", pathSet, null, ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testEmptySetGetEncodeOne() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testEmptySetGetEncodeTwo() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testEmptySetGetEncodeThree() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testEmptySetGetEncodeFour() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testEmptySetFormDataEncodeOne() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetFormDataEncodeTwo() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetFormDataEncodeThree() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetFormDataEncodeFour() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testEmptySetJSON() throws Exception {
        _verify("[]", pathSet, C.list(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

    @Test
    public void testNonEmptySetGetEncodeOne() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.ONE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetGetEncodeTwo() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.TWO, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetGetEncodeThree() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.THREE, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetGetEncodeFour() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.GET);
    }

    @Test
    public void testNonEmptySetFormDataEncodeOne() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.ONE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeTwo() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.TWO, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeThree() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.THREE, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeFour() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.FOUR, RequestMethod.POST_FORM_DATA);
    }

    @Test
    public void testNonEmptySetFormDataEncodeJSON() throws Exception {
        _verify(es(), pathSet, nonEmptyList(), ParamEncoding.JSON, RequestMethod.POST_JSON);
    }

}
