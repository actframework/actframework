package testapp.endpoint;

import org.junit.Test;

public class GHIssue222 extends EndpointTester {

    @Test
    public void testWithInterceptor() throws Exception {
        url("/gh/222").get();
        bodyEq("");
    }

}
