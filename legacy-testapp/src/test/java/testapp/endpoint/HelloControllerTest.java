package testapp.endpoint;

import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;
import testapp.TestApp;

import static org.osgl.http.H.Header.Names.*;
import static testapp.TestApp.GLOBAL_CORS.*;

public class HelloControllerTest extends EndpointTester {

    @Test
    public void testHello1() throws Exception {
        url("hello1");
        bodyEq("hello");
        verifyGlobalCORS();
    }

    @Test
    public void testHello2() throws Exception {
        url("hello2");
        bodyEq("hello");
        verifyGlobalCORS();
    }

    @Test
    public void testHello3() throws Exception {
        url("hello3");
        bodyEq(C.Map("hello", "hello"));
        verifyGlobalCORS();
    }

    @Test
    public void testHello4() throws Exception {
        url("hello4");
        bodyEq("hello");
        verifyGlobalCORS();
    }

    @Test
    public void testHello5() throws Exception {
        url("hello5").get("toWho", "world");
        bodyContains("Hello world");
        verifyGlobalCORS();
    }

    @Test
    public void testHello6() throws Exception {
        url("hello6").post("i", 5);
        bodyEq(5);
        verifyGlobalCORS();
    }

    @Test
    public void testGlobalCORS() throws Exception {
        url("hello1").options();
        verifyGlobalCORS();
    }


    private void verifyGlobalCORS() throws Exception {
        checkHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ORIGIN);
        if (reqBuilder.method() == H.Method.OPTIONS) {
            checkHeader(ACCESS_CONTROL_ALLOW_HEADERS, ALLOW_HEADER);
            checkHeader(ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSE_HEADER);
            checkHeader(ACCESS_CONTROL_MAX_AGE, MAX_AGE);
            checkHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET");
        }
    }

}
