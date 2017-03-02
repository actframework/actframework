package testapp.endpoint;

import org.junit.Test;
import org.osgl.mvc.result.NotFound;

import java.io.IOException;

public class VirtualHandlerTest extends EndpointTester {

    @Test
    public void virtualHandlerShallWorkAsExpected() throws IOException {
        url("/vc/1").get();
        bodyEq("1VirtualControllerTestBed");
        setup();
        url("/vc/foo/1").get();
        bodyEq("1Foo");
        setup();
        url("/vc/bar/1").get();
        bodyEq("1Bar");
    }

    @Test
    public void nonVirtualHandlerShallWorkAsExpectedIfSentToDeclaringClass() throws IOException {
        url("/vc/2").get();
        bodyEq("2VirtualControllerTestBed");
    }

    @Test(expected = NotFound.class)
    public void nonVirtualHandlerShallNotBeReachedIfSentToSubClass() throws IOException {
        url("/vc/foo/2").get();
        bodyEq("1Foo");
    }

}
