package testapp.endpoint;

import org.osgl.util.S;
import testapp.EndpointTester;

abstract class ParameterResolverTestBase extends EndpointTester {

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

    protected void notFound(String url0, String key, Object val, Object ... otherPairs) throws Exception {
        String url = processUrl(url0);
        notFoundByAllMethods(url, key, val, otherPairs);
    }

    protected void badRequest(String url0, String key, Object val, Object ... otherPairs) throws Exception {
        String url = processUrl(url0);
        badRequestByAllMethods(url, key, val, otherPairs);
    }

    protected static String s(Object o) {
        return S.string(o);
    }
}
