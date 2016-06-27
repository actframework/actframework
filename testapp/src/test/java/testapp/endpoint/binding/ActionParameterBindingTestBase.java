package testapp.endpoint.binding;

import org.junit.Before;
import testapp.endpoint.EndPointTestContext;
import testapp.endpoint.EndpointTester;

public abstract class ActionParameterBindingTestBase extends EndpointTester {

    protected EndPointTestContext context;

    @Before
    public final void initContext() {
        context = new EndPointTestContext();
    }

    protected abstract String urlContext();

    protected final String processUrl(String url) {
        String context = urlContext();
        if (!url.startsWith(context)) {
            url = context + (url.startsWith("/") ? url : "/" + url);
        }
        return url;
    }

}
