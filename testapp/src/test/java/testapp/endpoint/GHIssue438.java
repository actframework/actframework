package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;

public class GHIssue438 extends EndpointTester {

    @Test
    public void testStyleASuccessPath() throws Exception {
        url("/gh/438/a/1234").get();
        bodyEq("1234");
    }

    @Test(expected = BadRequest.class)
    public void testStyleABadCase1() throws Exception {
        url("/gh/438/a/12345").get();
        checkRespCode();
    }

    @Test(expected = BadRequest.class)
    public void testStyleABadCase2() throws Exception {
        url("/gh/438/a/12e4").get();
        checkRespCode();
    }

    @Test
    public void testStyleBSuccessPath() throws Exception {
        url("/gh/438/b/z").get();
        bodyEq("z");
        reset();
        url("/gh/438/b/a3_2z").get();
        bodyEq("a3_2z");
    }


    @Test(expected = BadRequest.class)
    public void testStyleB_BadCase1() throws Exception {
        url("/gh/438/b/1").get();
        checkRespCode();
    }

    @Test(expected = BadRequest.class)
    public void testStyleB_BadCase2() throws Exception {
        url("/gh/438/b/a-b").get();
        checkRespCode();
    }

}
