package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.BadRequest;
import org.osgl.util.C;

public class InterceptorTest extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("aop/foo?n=100");
        bodyEq("100");
        eq("200", resp().header("foo-code"));
    }

    @Test(expected = BadRequest.class)
    public void testFooFailed() throws Exception {
        url("aop/foo?n=-100");
        bodyEq("-100");
    }

    @Test
    public void testBarArray() throws Exception {
        url("aop/bar?n=20");
        bodyEq("array:20");
    }

    @Test
    public void testBarUnexpected() throws Exception {
        url("aop/bar?n=21");
        bodyEq("unexpected:21");
    }

    @Test
    public void testBarException() throws Exception {
        url("aop/bar?n=23");
        bodyEq("bar-exception:23");
    }

}
