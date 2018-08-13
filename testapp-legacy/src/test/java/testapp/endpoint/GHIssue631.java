package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.NotFound;

public class GHIssue631 extends EndpointTester {

    @Test
    public void test1() throws Exception {
        url("/gh/631/Accept?act_header_accept=application/json").get();
        bodyEq("{\"result\":\"application/json\"}");
    }

    @Test
    public void test2() throws Exception {
        url("/gh/631/X-Foo-Bar?act_header_x_foo_bar=123").get();
        bodyEq("123");
    }

    @Test(expected = NotFound.class)
    public void testProtectedHeader() throws Exception {
        url("/gh/631/Authorization?act_header_authorization=123").get();
        this.checkRespCode();
    }

}
