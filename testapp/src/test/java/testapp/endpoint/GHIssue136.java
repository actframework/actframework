package testapp.endpoint;

import org.junit.Test;

public class GHIssue136 extends EndpointTester {

    @Test
    public void testWithInterceptor() throws Exception {
        url("/gh/136/with").get();
        bodyEq("intercepted");
    }

    @Test
    public void testWithoutInterceptor() throws Exception {
        url("/gh/136/without").get();
        bodyEq("bar");
    }

}
