package testapp.endpoint;

import org.junit.Test;

public class GHIssue506 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/506").get();
        bodyEq("100");
    }
}
