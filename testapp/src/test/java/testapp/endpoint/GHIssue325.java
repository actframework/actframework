package testapp.endpoint;

import org.junit.Test;

public class GHIssue325 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/325/data/foo=3;bar=6/key/bar").get();
        bodyEq("6");
    }

}
