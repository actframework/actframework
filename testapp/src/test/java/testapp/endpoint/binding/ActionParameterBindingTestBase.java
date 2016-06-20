package testapp.endpoint.binding;

import org.osgl.mvc.result.BadRequest;
import org.osgl.mvc.result.NotFound;
import org.osgl.util.S;
import testapp.endpoint.EndpointTester;

public abstract class ActionParameterBindingTestBase extends EndpointTester {

    protected abstract String urlContext();

    protected String processUrl(String url) {
        String context = urlContext();
        if (!url.startsWith(context)) {
            url = context + (url.startsWith("/") ? url : "/" + url);
        }
        return url;
    }

    protected void verify(String expected, String url0, String key, Object val, Object ... otherPairs) throws Exception {
        String url = processUrl(url0);
        verifyAllMethods(expected, url, key, val, otherPairs);
    }

    protected static String s(Object o) {
        return S.string(o);
    }

    protected void badRequest(String url, String key, Object val) throws Exception {
        try {
            verify("", url, key, val);
            fail("It shall get 400 bad request response");
        } catch (BadRequest badRequest) {
            // pass
        }
    }

    protected void notFound(String url, String key, Object val) throws Exception {
        try {
            verify("", url, key, val);
            fail("It shall get 404 not found response");
        } catch (NotFound notFound) {
            // pass
        }
    }
}
