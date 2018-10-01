package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;

public class GHIssue691 extends EndpointTester {

    @Test
    public void test1() throws Exception {
        url("/gh/691/1?timestamp=100000000").get();
        checkRespCode();
    }

    @Test(expected = BadRequest.class)
    public void test1OutOfRange() throws Exception {
        url("/gh/691/1?timestamp=10000000").get();
        checkRespCode();
    }

    @Test
    public void test2() throws Exception {
        url("/gh/691/2?timestamp=2500000000").get();
        checkRespCode();
    }

    @Test(expected = BadRequest.class)
    public void test2OutOfRange() throws Exception {
        url("/gh/691/2?timestamp=12500000000").get();
        checkRespCode();
    }
}
