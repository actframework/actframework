package testapp.endpoint;

import org.junit.Test;
import org.osgl.util.C;
import testapp.EndpointTester;

import java.util.Map;

public class ParameterResolverTest extends EndpointTester {


    @Test
    public void testTakePrimitiveChar() throws Exception {
        final String url = "/pr/char_p";
        final String key = "c";
        verify("c", url, key, "c");
        verify("a", url, key, "abc");
        verify("", url, "a", "b");
    }

    @Test
    public void testTakeChar() throws Exception {
        final String url = "/pr/char";
        final String key = "c";
        verify("c", url, key, "c");
        verify("a", url, key, "abc");
        notFound(url, "a", "b");
    }

    @Test
    public void testTakePrimitiveInt() throws Exception {
        final String url = "/pr/int_p";
        final String key = "i";
        verify("4", url, key, 4);
        verify("234", url, key, 234);
        verify("0", url, "foo", "bar");
    }

    @Test
    public void testTakeInt() throws Exception {
        final String url = "/pr/int";
        final String key = "i";
        verify("4", url, key, 4);
        verify("234", url, key, 234);
        notFound(url, "foo", "bar");
    }


    private void verify(String expected, String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).get(key, val, otherPairs);
        bodyEq(expected);

        setup();
        url(url).post(key, val, otherPairs);
        bodyEq(expected);

        setup();
        Map<String, Object> params = C.newMap(key, val);
        Map<String, Object> otherParams = C.map(otherPairs);
        params.putAll(otherParams);
        url(url).postJSON(params);
        bodyEq(expected);
    }

    private void notFound(String url, String key, Object val, Object ... otherPairs) throws Exception {
        setup();
        url(url).get(key, val, otherPairs);
        notFound();

        setup();
        url(url).post(key, val, otherPairs);
        notFound();

        setup();
        Map<String, Object> params = C.newMap(key, val);
        Map<String, Object> otherParams = C.map(otherPairs);
        params.putAll(otherParams);
        url(url).postJSON(params);
        notFound();
    }
}
