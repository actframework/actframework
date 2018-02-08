package testapp.endpoint;

import org.junit.Test;

public class GHIssue504 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/504").get();
        bodyEq("10");
    }
}
