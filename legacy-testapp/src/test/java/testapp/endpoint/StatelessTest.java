package testapp.endpoint;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;

public class StatelessTest extends EndpointTester {

    @Test
    public void testControllerStateless() throws Exception {
        testUrl("stateless", true);
    }

    @Test
    public void testStatelessSingletonRegistration() throws Exception {
        testUrl("stateless/foo");
    }

    @Test
    public void testInheritedStatelessSingletonRegistration() throws Exception {
        testUrl("stateless/bar");
    }

    @Test
    public void testStopInheriedScope() throws Exception {
        testUrl("stateless/stopInheritedScope", true);
    }

    @Test
    public void testEagerSingleton() throws Exception {
        testUrl("stateless/eager");
    }

    @Test
    public void testLazySingleton() throws Exception {
        testUrl("stateless/lazy");
    }

    private void testUrl(String url) throws Exception {
        testUrl(url, false);
    }

    private void testUrl(String url, boolean revert) throws Exception {
        url(url).get();
        Response resp = resp();
        ResponseBody body = resp.body();
        String first = body.string();
        setup();
        url(url).get();
        String second = resp().body().string();
        if (!revert) {
            eq(first, second);
        } else {
            assertNotEquals(first, second);
        }
    }


}
