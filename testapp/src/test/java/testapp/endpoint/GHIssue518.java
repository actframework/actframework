package testapp.endpoint;

import org.junit.Test;

public class GHIssue518 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/518/Hi").get();
        bodyEq("Hi");
    }
}
