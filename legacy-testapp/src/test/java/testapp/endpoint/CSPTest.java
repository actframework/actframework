package testapp.endpoint;

import static org.osgl.http.H.Header.Names.*;

import org.junit.Test;

public class CSPTest extends EndpointTester {

    @Test
    public void testGlobal() throws Exception {
        url("/gh/136/with").get();
        checkHeader(CONTENT_SECURITY_POLICY, CSPTestBed.GLOBAL_SETTING);
    }

    @Test
    public void testEntry() throws Exception {
        url("csp").get();
        checkHeader(CONTENT_SECURITY_POLICY, CSPTestBed.CONTROLLER_SETTING);
    }

    @Test
    public void testFoo() throws Exception {
        url("csp/foo").get();
        checkHeader(CONTENT_SECURITY_POLICY, CSPTestBed.FOO_METHOD_SETTING);
    }

    @Test
    public void testBar() throws Exception {
        url("csp/bar").get();
        checkHeader(CONTENT_SECURITY_POLICY, null);
    }

}
