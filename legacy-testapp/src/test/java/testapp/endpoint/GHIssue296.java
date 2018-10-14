package testapp.endpoint;

import org.junit.Test;

public class GHIssue296 extends EndpointTester {

    @Test
    public void test() throws Exception {
        url("/gh/296?person[0].name=abc").post();
        bodyContains("abc");
    }

}
