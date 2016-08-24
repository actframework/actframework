package testapp.endpoint;

import org.junit.Test;
import org.osgl.http.H;
import org.osgl.util.C;

import static org.osgl.http.H.Header.Names.*;
import static testapp.TestApp.GLOBAL_CORS.*;

public class CORSTest extends EndpointTester {

    @Test
    public void testFoo() throws Exception {
        url("cors/foo").options();
        checkHeader(ACCESS_CONTROL_ALLOW_ORIGIN, CORSTestBed.FOO_ALLOW_ORIGIN);
        checkHeader(ACCESS_CONTROL_ALLOW_HEADERS, CORSTestBed.ALLOW_HEADERS);
        checkHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET");
        checkHeader(ACCESS_CONTROL_MAX_AGE, MAX_AGE);
        checkHeader(ACCESS_CONTROL_EXPOSE_HEADERS, ALLOW_EXPOSE_HEADER);
    }

    @Test
    public void testBar() throws Exception {
        url("cors/bar").options();
        assertNoHeader(ACCESS_CONTROL_ALLOW_ORIGIN);
        assertNoHeader(ACCESS_CONTROL_ALLOW_HEADERS);
        assertNoHeader(ACCESS_CONTROL_EXPOSE_HEADERS);
        assertNoHeader(ACCESS_CONTROL_MAX_AGE);
        assertNoHeader(ACCESS_CONTROL_ALLOW_METHODS);
    }

}
