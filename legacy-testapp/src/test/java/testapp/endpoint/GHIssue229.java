package testapp.endpoint;

import org.junit.Test;

public class GHIssue229 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/229/someone%2B1%40somewhere.com").get();
        bodyEq("someone+1@somewhere.com");
    }

}
