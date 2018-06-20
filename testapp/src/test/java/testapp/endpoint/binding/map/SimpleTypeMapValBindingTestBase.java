package testapp.endpoint.binding.map;

import static testapp.endpoint.ParamEncoding.*;

import org.junit.Test;
import org.osgl.util.C;
import testapp.endpoint.EndPointTestContext.RequestMethod;
import testapp.endpoint.ParamEncoding;
import testapp.endpoint.binding.ActionParameterBindingTestBase;

import java.util.Map;
import java.util.TreeMap;

public abstract class SimpleTypeMapValBindingTestBase<T> extends ActionParameterBindingTestBase {
    protected static final String PARAM = "v";
    private String path_v;
    private String path_k;

    public SimpleTypeMapValBindingTestBase(String path_v, String path_k) {
        this.path_v = path_v;
        this.path_k = path_k;
    }

    @Override
    protected final String urlContext() {
        return "/smpr";
    }

    public abstract Map<String, T> nonEmptyMap();

    public Map<T, String> keyTypedNonEmptyMap() {
        return flip(nonEmptyMap());
    }

    protected final String expectedRespForNonEmptyMap() {
        return com.alibaba.fastjson.JSON.toJSONString(new TreeMap<String, T>(nonEmptyMap()));
    }

    protected final String expectedRespForKeyTypedNonEmptyMap() {
        return com.alibaba.fastjson.JSON.toJSONString(new TreeMap<T, String>(keyTypedNonEmptyMap()));
    }

    @Test
    public final void testNullMapGetMethodTwo() throws Exception {
        _verify("{}", path_v, null, RequestMethod.GET, TWO);
    }

    @Test
    public final void testNullMapGetMethodFour() throws Exception {
        _verify("{}", path_v, null, RequestMethod.GET, FOUR);
    }

    @Test
    public final void testNullMapPostMethodTwo() throws Exception {
        _verify("{}", path_v, null, RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public final void testNullMapPostMethodFour() throws Exception {
        _verify("{}", path_v, null, RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testNullMapPostMethodJson() throws Exception {
        _verify("{}", path_v, null, RequestMethod.POST_JSON, JSON);
    }

    @Test
    public final void testNullTypedKeyMapGetMethodTwo() throws Exception {
        _verify("{}", path_k, null, RequestMethod.GET, TWO);
    }

    @Test
    public final void testNullTypedKeyMapGetMethodFour() throws Exception {
        _verify("{}", path_k, null, RequestMethod.GET, FOUR);
    }

    @Test
    public final void testNullTypedKeyMapPostMethodTwo() throws Exception {
        _verify("{}", path_k, null, RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public final void testNullTypedKeyMapPostMethodFour() throws Exception {
        _verify("{}", path_k, null, RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testNullTypedKeyMapPostMethodJson() throws Exception {
        _verify("{}", path_k, null, RequestMethod.POST_JSON, JSON);
    }

    @Test
    public final void testEmptyMapGetTwo() throws Exception {
        _verify("{}", path_v, C.<String, T>Map(), RequestMethod.GET, TWO);
    }

    @Test
    public final void testEmptyMapGetFour() throws Exception {
        _verify("{}", path_v, C.<String, T>Map(), RequestMethod.GET, FOUR);
    }

    @Test
    public final void testEmptyMapPostTwo() throws Exception {
        _verify("{}", path_v, C.<String, T>Map(), RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public final void testEmptyMapPostFour() throws Exception {
        _verify("{}", path_v, C.<String, T>Map(), RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testEmptyMapPostJson() throws Exception {
        _verify("{}", path_v, C.<String, T>Map(), RequestMethod.POST_JSON, JSON);
    }

    @Test
    public final void testTypedKeyEmptyMapGetTwo() throws Exception {
        _verify("{}", path_k, C.<String, T>Map(), RequestMethod.GET, TWO);
    }

    @Test
    public final void testTypedKeyEmptyMapGetFour() throws Exception {
        _verify("{}", path_k, C.<String, T>Map(), RequestMethod.GET, FOUR);
    }

    @Test
    public final void testTypedKeyEmptyMapPostTwo() throws Exception {
        _verify("{}", path_k, C.<String, T>Map(), RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public final void testTypedKeyEmptyMapPostFour() throws Exception {
        _verify("{}", path_k, C.<String, T>Map(), RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testTypedKeyEmptyMapPostJson() throws Exception {
        _verify("{}", path_k, C.<String, T>Map(), RequestMethod.POST_JSON, JSON);
    }

    @Test
    public final void testNonEmptyMapGetTwo() throws Exception {
        _verify(ev(), path_v, nonEmptyMap(), RequestMethod.GET, TWO);
    }

    @Test
    public final void testNonEmptyMapGetFour() throws Exception {
        _verify(ev(), path_v, nonEmptyMap(), RequestMethod.GET, FOUR);
    }

    @Test
    public final void testNonEmptyMapPostTwo() throws Exception {
        _verify(ev(), path_v, nonEmptyMap(), RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public final void testNonEmptyMapPostFour() throws Exception {
        _verify(ev(), path_v, nonEmptyMap(), RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testNonEmptyMapPostJson() throws Exception {
        _verify(ev(), path_v, nonEmptyMap(), RequestMethod.POST_JSON, JSON);
    }

    @Test
    public final void testKeyTypedNonEmptyMapGetTwo() throws Exception {
        _verify(ek(), path_k, keyTypedNonEmptyMap(), RequestMethod.GET, TWO);
    }

    @Test
    public void testKeyTypedNonEmptyMapGetFour() throws Exception {
        _verify(ek(), path_k, keyTypedNonEmptyMap(), RequestMethod.GET, FOUR);
    }

    @Test
    public final void testKeyTypedNonEmptyMapPostTwo() throws Exception {
        _verify(ek(), path_k, keyTypedNonEmptyMap(), RequestMethod.POST_FORM_DATA, TWO);
    }

    @Test
    public void testKeyTypedNonEmptyMapPostFour() throws Exception {
        _verify(ek(), path_k, keyTypedNonEmptyMap(), RequestMethod.POST_FORM_DATA, FOUR);
    }

    @Test
    public final void testKeyTypedNonEmptyMapPostJson() throws Exception {
        _verify(ek(), path_k, keyTypedNonEmptyMap(), RequestMethod.POST_JSON, JSON);
    }

    private String ev() {
        return expectedRespForNonEmptyMap();
    }

    private String ek() {
        return expectedRespForKeyTypedNonEmptyMap();
    }

    private void _verify(
            String expected,
            String urlPath,
            Map<?, ?> data,
            RequestMethod method,
            ParamEncoding encoding
    ) throws Exception {
        context.expected(expected).url(processUrl(urlPath)).params(encoding.encode(PARAM, data)).method(method).applyTo(this);
    }

    protected static <T> Map<T, String> flip(Map<String, T> map) {
        Map ret = C.newMap();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            ret.put(entry.getValue(), entry.getKey());
        }
        return ret;
    }

}
