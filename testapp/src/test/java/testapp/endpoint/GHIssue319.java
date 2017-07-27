package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;

public class GHIssue319 extends EndpointTester {

    @Test(expected = BadRequest.class)
    public void test() throws Exception {
        url("/gh/319").getJSON();
        String s = resp().body().string();
        System.out.println(s);
        checkRespCode();
    }

}
