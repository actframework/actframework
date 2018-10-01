package testapp.endpoint;

import org.junit.Test;

public class GHIssue354 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/354").get();
        String body = resp().body().string();
        yes(body.contains("4"));
        yes(body.contains("/gh/354"));
    }

}
