package testapp.endpoint;

import org.junit.Test;

public class GHIssue295 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/295/done").get();
        bodyEq("done");
    }

}
