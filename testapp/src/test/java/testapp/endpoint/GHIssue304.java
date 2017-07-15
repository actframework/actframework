package testapp.endpoint;

import org.junit.Test;

public class GHIssue304 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/304").get();
        bodyEq("true");
    }

}
