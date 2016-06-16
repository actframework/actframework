package testapp.endpoint;

import org.junit.Test;
import org.osgl.util.C;
import testapp.EndpointTester;

public class HelloControllerTest extends EndpointTester {

    @Test
    public void testHello1() throws Exception {
        url("hello1");
        bodyEq("hello");
    }

    @Test
    public void testHello2() throws Exception {
        url("hello2");
        bodyEq("hello");
    }

    @Test
    public void testHello3() throws Exception {
        url("hello3");
        bodyEq(C.map("hello", "hello"));
    }

    @Test
    public void testHello4() throws Exception {
        url("hello4");
        bodyEq("hello");
    }

    @Test
    public void testHello5() throws Exception {
        url("hello5").get("toWho", "world");
        bodyContains("Hello world");
    }

    @Test
    public void testHello6() throws Exception {
        url("hello6").post("i", 5);
        bodyEq(5);
    }

}
