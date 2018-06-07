package testapp.endpoint;

import org.junit.Test;

public class GHIssue678 extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("/gh/678").get();
        eq("678", resp().header("H678"));
    }

}
