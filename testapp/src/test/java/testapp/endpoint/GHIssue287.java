package testapp.endpoint;

import org.junit.Test;

public class GHIssue287 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/287").get();
        bodyContains("6");
    }

}
