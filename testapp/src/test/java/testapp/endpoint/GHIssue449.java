package testapp.endpoint;

import org.junit.Test;

public class GHIssue449 extends EndpointTester {

    @Test()
    public void testFoo() throws Exception {
        url("/gh/449/?data[name]=foo").get();
        bodyEq("foo");
    }

}
