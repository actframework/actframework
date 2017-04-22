package testapp.endpoint.issue.enhancement;

import org.junit.Test;
import testapp.endpoint.EndpointTester;

public class GHIssue152 extends EndpointTester {

    @Test
    public void testGlobalOnClass() throws Exception {
        url("/gh/152").get();
        bodyEq("intercepted");
    }

    @Test
    public void testGlobalOnMethod() throws Exception {
        url("/gh/152/method").get();
        bodyEq("intercepted");
    }

}
